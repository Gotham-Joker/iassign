create table sys_dept
(
	id varchar(64) not null
		primary key,
	dept_id varchar(64) null,
	dept_code varchar(20) null,
	dept_tw varchar(100) null,
	create_time datetime null,
	update_time datetime null
)
comment '部门表';

create table sys_menu
(
	id varchar(20) not null
		primary key,
	pid varchar(20) null,
	text varchar(20) null,
	icon varchar(10) null,
	link varchar(250) null,
	weight int null,
	all_available tinyint null comment '是否所有人可见'
)
charset=utf8mb4;

create table sys_message
(
	id varchar(20) not null
		primary key,
	subject varchar(250) null comment '主题',
	type tinyint(2) null comment '消息类型 1-text 2-link 3-card',
	cover varchar(255) null comment 'card的封面',
	content text null comment '消息内容',
	link varchar(255) null comment '链接',
	status tinyint(2) null comment '0-未读 1-已读 2-逻辑删除',
	to_user_id varchar(20) null,
	from_user_id varchar(20) null,
	from_username varchar(64) null,
	from_user_avatar varchar(255) null,
	create_time datetime null
)
comment '系统消息';

create index sys_message_user_id_index
	on sys_message (to_user_id);

create table sys_permission
(
	id varchar(20) not null
		primary key,
	scope varchar(20) null,
	name varchar(32) null,
	mark varchar(20) null comment '权限标识',
	url varchar(250) null,
	method varchar(10) null
)
charset=utf8mb4;

create table sys_role
(
	id varchar(20) not null
		primary key,
	name varchar(20) null
)
charset=utf8mb4;

create table sys_role_menu
(
	role_id varchar(20) null,
	menu_id varchar(20) null
)
comment '角色-菜单 关联表' charset=utf8mb4;

create table sys_role_permission
(
	role_id varchar(20) null,
	permission_id varchar(20) null
)
comment '角色-权限 关联表' charset=utf8mb4;

create table sys_user
(
	id varchar(20) not null
		primary key,
	username varchar(20) null,
	email varchar(64) null,
	avatar varchar(64) null,
	dept_id varchar(20) null,
	dept_code varchar(20) null,
	dept_name varchar(20) null,
	fova_ao varchar(20) null,
	stock_ao varchar(20) null,
	admin tinyint default 0 null,
	create_time datetime null,
	update_time datetime null,
	staff_id varchar(10) null
);

create table sys_user_role
(
	user_id varchar(20) null,
	role_id varchar(20) null,
	primary key (user_id,role_id)
)
comment '用户-角色 关联表' charset=utf8mb4;

create table t_form_definition
(
	id varchar(20) not null
		primary key,
	name varchar(100) not null,
	description varchar(200) null,
	definition longtext collate utf8mb4_general_ci null,
	creator varchar(20) null,
	create_time datetime null,
	update_time datetime null
)
charset=utf8mb4;

create table t_form_instance
(
	id varchar(20) not null
		primary key,
	form_definition_id varchar(20) null,
	data longtext collate utf8mb4_general_ci null,
	type tinyint null,
	create_time datetime null,
	update_time datetime null,
	variables longtext null comment '从data中解析出来的键值对'
)
charset=utf8mb4;

create table t_process_definition
(
	id varchar(20) not null
		primary key,
	name varchar(255) null,
	status tinyint null,
	seq_no varchar(10) null comment '序号，有时候记住序号比记住流程名有用，例如0001代表休假申请',
	icon varchar(250) null comment '流程图标',
	form_id varchar(20) null,
	description varchar(255) null,
	creator varchar(20) null,
	create_time datetime null,
	update_time datetime null,
	group_name varchar(255) null,
	ru_id varchar(48) null comment '总是关联最新的流程图版本'
)
charset=utf8mb4;

create index idx_name
	on t_process_definition (name);

create table t_process_definition_auth
(
	definition_id varchar(20) not null,
	dept_id varchar(20) not null,
	primary key (definition_id, dept_id)
)
comment '流程定义与部门关系表' charset=utf8mb4;

create table t_process_definition_ru
(
	id varchar(64) not null
		primary key,
	definition_id varchar(20) null comment '流程定义id',
	create_time datetime null,
	dag longtext null comment 'DAG图'
)
comment '运行时的流程定义dag表';

create table t_process_instance
(
	id varchar(20) not null
		primary key,
	definition_id varchar(20) null,
	name varchar(64) null,
	starter varchar(20) null comment '发起人ID',
	starter_name varchar(64) null comment '发起人姓名',
	dept_id varchar(32) null comment '部门ID',
	handler_id varchar(20) null comment '当前处理人',
	handler_name varchar(64) null,
	pre_handler_id varchar(20) null comment '上一处理人',
	form_instance_id varchar(20) null comment '表单实例id',
	variable_id varchar(20) null comment '变量ID',
	dag_node_id varchar(32) null comment 'dag节点ID',
	status tinyint null comment '状态：0-已撤回 1-运行中 2-成功 3-失败',
	create_time datetime null,
	update_time datetime null,
	ru_id varchar(64) null comment '运行时id，关联t_process_definition_ru表',
	emails longtext null comment '邮件发送清单'
)
comment '流程实例表' charset=utf8mb4;

create table t_process_task
(
	id varchar(20) not null
		primary key,
	definition_id varchar(20) null comment '工作流定义ID',
	instance_id varchar(20) null comment '工作流实例ID',
	form_id varchar(20) null comment '表单定义ID',
	form_instance_id varchar(20) null comment '表单实例ID',
	variable_id varchar(20) null comment '变量ID',
	name varchar(255) null comment '任务名称',
	assign_id varchar(20) null comment '指派用户ID',
	handler_id varchar(20) null comment '受理人ID',
	pre_handler_id varchar(20) null comment '上一受理人ID',
	income_id varchar(64) null comment '入口连接线ID',
	dag_node_id varchar(20) null comment 'dag节点ID',
	remark text null comment '备注',
	assign_remark text null comment '指派人的备注',
	attachments text null comment '附件清单',
	status tinyint null comment 'NONE(0)-无 PENDING(1)-待认领 CLAIMED(2)-已受理 ASSIGNED(3)-已指派 SUCCESS(4)-审批通过 REJECTED(5)-拒绝 BACK(6)-退回 FAILED(7)-失败',
	create_time datetime null,
	assign_time datetime null comment '受理时间或指派时间 指派其实就默认受理了之后再转交给别人，所以时间用同一个字段',
	update_time datetime null,
	user_node tinyint not null comment '是否是用户审批环节：0-不是（系统自动处理） 1：是',
	handler_avatar varchar(255) null,
	handler_name varchar(20) null,
	handler_email varchar(255) null,
	assign_avatar varchar(250) null comment '被指派人头像',
	assign_name varchar(20) null,
	assign_email varchar(255) null
)
comment '流程任务表' charset=utf8mb4;

create table t_process_task_auth
(
	id varchar(20) not null
		primary key,
	reference_id varchar(20) null,
	name varchar(100) null,
	task_id varchar(20) null,
	type tinyint null comment '0-用户 1-角色',
	avatar varchar(255) null comment '用户头像'
)
comment '任务授权表' charset=utf8mb4;

create index t_process_task_auth_task_id_index
	on t_process_task_auth (task_id);

create table t_process_variables
(
	id varchar(20) not null
		primary key,
	instance_id varchar(20) null,
	data longtext collate utf8mb4_general_ci null
)
comment '流程变量表' charset=utf8mb4;

INSERT INTO sys_menu (id, pid, text, icon, link, weight, all_available) VALUES ('1563424145789804546', '0', 'WORK FLOW', null, null, 8000, 1);
INSERT INTO sys_menu (id, pid, text, icon, link, weight, all_available) VALUES ('1563425283947749377', '1563424145789804546', '工作流', 'gateway', null, 8100, 1);
INSERT INTO sys_menu (id, pid, text, icon, link, weight, all_available) VALUES ('1563425482220888065', '1563425283947749377', '流程管理', null, '/process/process-manage', 8200, 1);
INSERT INTO sys_menu (id, pid, text, icon, link, weight, all_available) VALUES ('1642105422302564354', '9100', '部门管理', null, '/system/dept', 9330, 0);
INSERT INTO sys_menu (id, pid, text, icon, link, weight, all_available) VALUES ('1642186485959905282', '1563425283947749377', '表单管理', null, '/form/form-manage', 8400, 0);
INSERT INTO sys_menu (id, pid, text, icon, link, weight, all_available) VALUES ('1642198788675620865', '1563425283947749377', '发起审批', null, '/process/process-list', 8150, 1);
INSERT INTO sys_menu (id, pid, text, icon, link, weight, all_available) VALUES ('1642207119851016193', '1563425283947749377', '我的申请', null, '/process/process-instance', 8160, 1);
INSERT INTO sys_menu (id, pid, text, icon, link, weight, all_available) VALUES ('1642207226415697922', '1563425283947749377', '我的待办', null, '/process/todo-list', 8170, 1);
INSERT INTO sys_menu (id, pid, text, icon, link, weight, all_available) VALUES ('9000', '0', 'SYSTEM', '', null, 9000, 0);
INSERT INTO sys_menu (id, pid, text, icon, link, weight, all_available) VALUES ('9100', '9000', '系统设置', 'setting', '', 9100, 0);
INSERT INTO sys_menu (id, pid, text, icon, link, weight, all_available) VALUES ('9200', '9100', '用户管理', null, '/system/user', 9200, 0);
INSERT INTO sys_menu (id, pid, text, icon, link, weight, all_available) VALUES ('9300', '9100', '角色管理', null, '/system/role', 9300, 0);
INSERT INTO sys_menu (id, pid, text, icon, link, weight, all_available) VALUES ('9400', '9100', '菜单管理', null, '/system/menu', 9400, 0);
INSERT INTO sys_menu (id, pid, text, icon, link, weight, all_available) VALUES ('9500', '9100', '权限管理', null, '/system/permission', 9500, 0);

INSERT INTO sys_permission (id, scope, name, mark, url, method) VALUES ('1563423133494534145', null, '添加用户', 'uadd', '/api/users', 'POST');
INSERT INTO sys_permission (id, scope, name, mark, url, method) VALUES ('1563423240075993090', null, '角色管理', 'rm', '/api/roles/**', '');
INSERT INTO sys_permission (id, scope, name, mark, url, method) VALUES ('1563423358409891841', null, '菜单管理', 'mm', '/api/menus/**', '');
INSERT INTO sys_permission (id, scope, name, mark, url, method) VALUES ('1563423479545585666', null, '权限管理', 'pm', '/api/permissions/**', '');
INSERT INTO sys_permission (id, scope, name, mark, url, method) VALUES ('1691299475558076418', null, '流程日志', 'plog', '/api/process-instance/log', 'GET');
INSERT INTO sys_permission (id, scope, name, mark, url, method) VALUES ('1696726880390119425', null, '表单管理', 'fm', '/api/forms', '');
INSERT INTO sys_permission (id, scope, name, mark, url, method) VALUES ('1696727478774693889', null, '流程部署', 'pdd', '/api/process-definition/deploy', '');
INSERT INTO sys_permission (id, scope, name, mark, url, method) VALUES ('1696727688028520449', null, '流程添加', 'pdadd', '/api/process-definition', 'POST');
INSERT INTO sys_permission (id, scope, name, mark, url, method) VALUES ('1696816914309619714', null, '删除用户', 'udel', '/api/users', 'DELETE');
INSERT INTO sys_permission (id, scope, name, mark, url, method) VALUES ('1696817042374303746', null, '修改用户', 'uupd', '/api/users', 'PUT');
INSERT INTO sys_permission (id, scope, name, mark, url, method) VALUES ('1696817192698159105', null, '设为管理员', 'admin', '/api/users/set-admin', 'PUT');
INSERT INTO sys_permission (id, scope, name, mark, url, method) VALUES ('1698598279797903362', null, '流程修改', 'pdmd', '/api/process-definition', 'PUT');
INSERT INTO sys_permission (id, scope, name, mark, url, method) VALUES ('1698598363331661826', null, '流程删除', 'pdrm', '/api/process-definition', 'DELETE');
INSERT INTO sys_permission (id, scope, name, mark, url, method) VALUES ('1698690283203850241', null, '指派', 'assign', '/api/process-task/assign', 'POST');
