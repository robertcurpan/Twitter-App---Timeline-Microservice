package com.twitterapp.timelinemicroservice.extrastructures;

public enum RolesEnum
{
    ADMIN(Integer.valueOf(5)),
    READER(Integer.valueOf(6)),
    WRITER(Integer.valueOf(7));

    private final Integer roleId;

    private RolesEnum(final Integer roleId) {
        this.roleId = roleId;
    }

    public Integer getRoleId() {
        return this.roleId;
    }
}
