package com.landfun.boot.modules.system.msg;

/**
 * When sending a message, who are the recipients.
 */
public enum MessageTargetType {
    /** Single user */
    USER,
    /** Users in one department */
    DEPT,
    /** Users in department and all sub-departments */
    DEPT_WITH_CHILDREN,
    /** All users in system */
    ALL,
    /** Users with a given role */
    ROLE,
    /** Explicit list of user IDs */
    USER_IDS
}
