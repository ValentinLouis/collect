/**
 *
 */
package org.openforis.idm.model.expression;

import static java.util.Arrays.asList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.jxpath.FunctionLibrary;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathContextFactory;
import org.apache.commons.jxpath.JXPathIntrospector;
import org.apache.commons.jxpath.JXPathInvalidSyntaxException;
import org.apache.commons.jxpath.ri.JXPathContextReferenceImpl;
import org.apache.commons.jxpath.ri.model.NodePointerFactory;
import org.apache.commons.lang3.StringUtils;
import org.openforis.idm.metamodel.validation.LookupProvider;
import org.openforis.idm.model.Node;
import org.openforis.idm.model.Record;
import org.openforis.idm.model.expression.internal.CustomFunctions;
import org.openforis.idm.model.expression.internal.IDMFunctions;
import org.openforis.idm.model.expression.internal.MathFunctions;
import org.openforis.idm.model.expression.internal.ModelJXPathCompiledExpression;
import org.openforis.idm.model.expression.internal.ModelJXPathContext;
import org.openforis.idm.model.expression.internal.ModelNodePointerFactory;
import org.openforis.idm.model.expression.internal.NodePropertyHandler;
import org.openforis.idm.model.expression.internal.RecordPropertyHandler;
import org.openforis.idm.model.expression.internal.ReferencedPathEvaluator;
import org.openforis.idm.model.expression.internal.RegExFunctions;
import org.openforis.idm.path.Path;

/**
 * @author M. Togna
 * @author S. Ricci
 */
public class ExpressionFactory {
	public static final String IDM_PREFIX = "idm";
	public static final String MATH_PREFIX = "math";
	public static final String REGEX_PREFIX = "regex";

	private static final Set<String> CORE_FUNCTION_NAMES = new HashSet<String>(asList(
			"boolean", "not", "true", "false", // boolean values functions
			"number", "round", "floor", "ceiling", //math functions
			"string", "concat", "substring", "string-length", "normalize-space", "contains", "starts-with", "ends-with", // string functions
			"count", "sum", // aggregate functions
			"position", "last" // context functions
	));

	private static final ExpressionCache COMPILED_EXPRESSIONS = new ExpressionCache();

	private final Map<String, CustomFunctions> customFunctionsByNamespace = new HashMap<String, CustomFunctions>();
	private final ReferencedPathEvaluator referencedPathEvaluator;
	private ModelJXPathContext jxPathContext;
	private LookupProvider lookupProvider;

	public ExpressionFactory() {
		System.setProperty(JXPathContextFactory.FACTORY_NAME_PROPERTY, "org.openforis.idm.model.expression.internal.ModelJXPathContextFactory");

		NodePointerFactory nodePointerFactory = new ModelNodePointerFactory();
		JXPathContextReferenceImpl.addNodePointerFactory(nodePointerFactory);

		JXPathIntrospector.registerDynamicClass(Node.class, NodePropertyHandler.class);
		JXPathIntrospector.registerDynamicClass(Record.class, RecordPropertyHandler.class);

		registerFunctions(
				new MathFunctions(MATH_PREFIX),
				new IDMFunctions(IDM_PREFIX),
				new RegExFunctions(REGEX_PREFIX)
		);

		referencedPathEvaluator = new ReferencedPathEvaluator(customFunctionsByNamespace);
	}

	public BooleanExpression createBooleanExpression(String expression) throws InvalidExpressionException {
		return createBooleanExpression(expression, false);
	}

	public BooleanExpression createBooleanExpression(String expression, boolean normalizeNumbers) throws InvalidExpressionException {
		ModelJXPathCompiledExpression compiledExpression = compileExpression(expression, normalizeNumbers);
		BooleanExpression expr = new BooleanExpression(compiledExpression, jxPathContext);
		return expr;
	}

	public ValueExpression createValueExpression(String expression) throws InvalidExpressionException {
		ModelJXPathCompiledExpression compiledExpression = compileExpression(expression);
		ValueExpression expr = new ValueExpression(compiledExpression, jxPathContext);
		return expr;
	}

	public ModelPathExpression createModelPathExpression(String expression) throws InvalidExpressionException {
		ModelJXPathCompiledExpression compiledExpression = compileExpression(expression);
		ModelPathExpression expr = new ModelPathExpression(compiledExpression, jxPathContext);
		return expr;
	}

	public AbsoluteModelPathExpression createAbsoluteModelPathExpression(String expression) throws InvalidExpressionException {
		if (!expression.startsWith("/")) {
			throw new InvalidExpressionException("Absolute paths must start with '/'");
		}
		int pos = expression.indexOf('/', 1);
		if (pos < 0) {
			String root = expression.substring(1);
			return new AbsoluteModelPathExpression(root);
		} else {
			String root = expression.substring(1, pos);
			expression = expression.substring(pos + 1);
			ModelJXPathCompiledExpression compiledExpression = compileExpression(expression);
			return new AbsoluteModelPathExpression(root, compiledExpression, jxPathContext);
		}
	}

	public LookupProvider getLookupProvider() {
		return lookupProvider;
	}

	public boolean isValidFunction(String namespace, String functionName) {
		boolean inDefaultNamespace = StringUtils.isBlank(namespace);
		if (inDefaultNamespace) {
			return CORE_FUNCTION_NAMES.contains(functionName);
		}
		CustomFunctions functions = customFunctionsByNamespace.get(namespace);
		return functions != null && functions.containsFunction(functionName);
	}

	public void setLookupProvider(LookupProvider lookupProvider) {
		this.lookupProvider = lookupProvider;
		jxPathContext.setLookupProvider(lookupProvider);
	}

	private void registerFunctions(CustomFunctions... functionsLibrary) {
		jxPathContext = (ModelJXPathContext) JXPathContext.newContext(null);
		FunctionLibrary library = new FunctionLibrary();
		for (CustomFunctions functions : functionsLibrary) {
			library.addFunctions(functions);
			String namespace = functions.getNamespace();
			if (customFunctionsByNamespace.containsKey(namespace)) {
				throw new IllegalStateException(String.format("Functions for namespace %s already registered", namespace));
			}
			customFunctionsByNamespace.put(namespace, functions);
		}
		jxPathContext.setFunctions(library);
	}

	private ModelJXPathCompiledExpression compileExpression(String expression) throws InvalidExpressionException {
		return compileExpression(expression, false);
	}

	private ModelJXPathCompiledExpression compileExpression(String expression, boolean normalizeNumber) throws InvalidExpressionException {
		String key = expressionKey(expression, normalizeNumber);
		ModelJXPathCompiledExpression compiled = COMPILED_EXPRESSIONS.get(key);
		if (compiled == null) {
			try {
				String normalizedExpression = Path.getNormalizedPath(expression);
				compiled = (ModelJXPathCompiledExpression) ModelJXPathContext.compile(referencedPathEvaluator, normalizedExpression, normalizeNumber);
				COMPILED_EXPRESSIONS.put(key, compiled);
			} catch (JXPathInvalidSyntaxException e) {
				throw new InvalidExpressionException(e.getMessage());
			}

		}
		return compiled;
	}

	private static String expressionKey(String expression, boolean normalizeNumbers) {
		return expression + "|||" + normalizeNumbers;
	}

	private static class ExpressionCache extends LinkedHashMap<String, ModelJXPathCompiledExpression> {

		private static final long serialVersionUID = 1L;

		private static final int MAX_ENTRIES = 1000;

		@Override
		protected boolean removeEldestEntry(java.util.Map.Entry<String, ModelJXPathCompiledExpression> eldest) {
			return size() > MAX_ENTRIES;
		}

	}

}
