package org.openforis.collect.model;

import org.apache.commons.lang3.StringUtils;
import org.openforis.collect.persistence.jooq.tables.pojos.OfcUserGroup;

public class UserGroup extends OfcUserGroup {

	private static final long serialVersionUID = 1L;

	public enum Visibility {
		PUBLIC('P'), PRIVATE('N');
		
		private char code;
		
		Visibility(char code) {
			this.code = code;
		}
		
		public static Visibility fromCode(char code) {
			for (Visibility value : values()) {
				if (value.code == code) {
					return value;
				}
			}
			throw new IllegalArgumentException("Invalid UserGroup Visibility code: " + code);
		}
		
		public char getCode() {
			return code;
		}
	}
	
	public enum UserGroupRequestStatus {
		ACCEPTED('A'), REJECTED('R'), PENDING('P');
		
		private char code;
		
		UserGroupRequestStatus(char code) {
			this.code = code;
		}
		
		public static UserGroupRequestStatus fromCode(char code) {
			for (UserGroupRequestStatus value : values()) {
				if (value.code == code) {
					return value;
				}
			}
			throw new IllegalArgumentException("Invalid UserGroupRequestStatus Visibility code: " + code);
		}
		
		public char getCode() {
			return code;
		}
	}
	
	public enum UserGroupRole {
		OWNER('O'), ADMINISTRATOR('A'), DATA_ANALYZER('D'), OPERATOR('U'), VIEWER('V');
		
		private char code;
		
		UserGroupRole(char code) {
			this.code = code;
		}
		
		public static UserGroupRole fromCode(char code) {
			for (UserGroupRole value : values()) {
				if (value.code == code) {
					return value;
				}
			}
			throw new IllegalArgumentException("Invalid UserGroupRole Visibility code: " + code);
		}
		
		public char getCode() {
			return code;
		}
	}
	
	private User createdByUser;

	public User getCreatedByUser() {
		return createdByUser;
	}

	public void setCreatedByUser(User createdByUser) {
		this.createdByUser = createdByUser;
	}

	public Visibility getVisibility() {
		String code = getVisibilityCode();
		return StringUtils.isBlank(code) ? null : Visibility.fromCode(code.charAt(0));
	}

	public void setVisibility(Visibility visibility) {
		setVisibilityCode(visibility == null ? null : String.valueOf(visibility.getCode()));
	}

}
