package org.mamotec.common.enums;

public enum AccessType {
	READ(0),
	WRITE(1),
	READ_WRITE(2);

	private final int value;

	private AccessType(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static AccessType fromValue(int value) {
		for (AccessType accessType : AccessType.values()) {
			if (accessType.getValue() == value) {
				return accessType;
			}
		}
		return null;
	}
}
