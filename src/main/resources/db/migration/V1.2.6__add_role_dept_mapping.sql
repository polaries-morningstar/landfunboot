DROP TABLE IF EXISTS sys_role_dept_mapping;

create table sys_role_dept_mapping(
    role_id bigint unsigned not null,
    dept_id bigint unsigned not null
) engine=innodb;

alter table sys_role_dept_mapping
    add constraint pk_sys_role_dept_mapping
        primary key(role_id, dept_id);

alter table sys_role_dept_mapping
    add constraint fk_sys_role_dept_mapping_role_id
        foreign key(role_id)
        references sys_role(id);

alter table sys_role_dept_mapping
    add constraint fk_sys_role_dept_mapping_dept_id
        foreign key(dept_id)
        references sys_dept(id);
