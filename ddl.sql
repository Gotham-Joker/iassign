create table oauth2_access_token
(
	access_token varchar(255) not null
		primary key,
	user_id varchar(64) null,
	client_id varchar(255) null,
	expire_in int(8) null,
	refresh_token varchar(255) null,
	refresh_token_create_time datetime null,
	access_token_create_time datetime null
)
collate=utf8mb4_general_ci;

create table oauth2_authorization_code
(
	code varchar(255) not null
		primary key,
	user_id varchar(255) null,
	client_id varchar(255) null,
	redirect_uri varchar(255) null,
	state varchar(255) null,
	create_time datetime null
)
collate=utf8mb4_general_ci;

create table oauth2_client
(
	client_id varchar(255) not null
		primary key,
	client_secret varchar(255) null,
	client_name varchar(255) null,
	redirect_uri varchar(255) null,
	mark varchar(255) null comment '备注',
	owner_id varchar(255) null
)
collate=utf8mb4_general_ci;

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
	user_id varchar(20) not null,
	role_id varchar(20) not null,
	primary key (user_id, role_id)
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
	fallback varchar(255) comment '流程失败时回调的接口'
	ru_id varchar(48) null comment '总是关联最新的流程图版本',
	managers varchar(100) null comment '流程数据管理者id列表',
	returnable tinyint(2) null comment '是否可退回申请人 0-否 1-是'
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
	emails longtext null comment '邮件发送清单',
    returnable tinyint(2) null comment '是否可退回申请人 0-否 1-是'
)
comment '流程实例表' charset=utf8mb4;

create index t_process_instance_definition_id_index
	on t_process_instance (definition_id);

create table t_process_opinion
(
	id varchar(20) not null
		primary key,
	instance_id varchar(20) null,
	task_id varchar(20) null,
	user_id varchar(20) null,
	username varchar(100) null,
	avatar varchar(255) null,
	email varchar(255) null,
	attachments text null,
	remark text null,
	create_time datetime null,
	assign_id varchar(20) null comment '被指派人ID',
	assign_name varchar(100) null comment '被指派人姓名',
	assign_avatar varchar(255) null comment '被指派人头像',
	assign_mail varchar(255) null comment '被指派人邮箱',
	operation tinyint(2) null comment '操作标志位 0-否决 1-同意 2-退回 3-指派'
)
comment '流程审批意见表';

create index t_process_opinion_instance_id_task_id_index
	on t_process_opinion (instance_id, task_id);

create index t_process_opinion_user_id_create_time_index
	on t_process_opinion (user_id, create_time);


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
	handler_id varchar(20) null comment '受理人ID',
	pre_handler_id varchar(20) null comment '上一受理人ID',
	income_id varchar(64) null comment '入口连接线ID',
	dag_node_id varchar(20) null comment 'dag节点ID',
	status tinyint null comment 'NONE(0)-无 PENDING(1)-待认领 CLAIMED(2)-已受理 ASSIGNED(3)-已指派 SUCCESS(4)-审批通过 REJECTED(5)-拒绝 BACK(6)-退回 FAILED(7)-失败',
	create_time datetime null,
	update_time datetime null,
	user_node tinyint not null comment '是否是用户审批环节：0-不是（系统自动处理） 1：是',
	countersign tinyint(2) null comment '会签标志 0-否 1-是',
	file_required tinyint(2) null comment '是否必须上传附件 0-否 1-是',
	assign tinyint(2) null comment '是否可以指派 0-否 1-是'
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

INSERT INTO sys_user (`id`, `username`, `email`, `avatar`, `dept_id`, `dept_code`, `dept_name`, `fova_ao`, `stock_ao`, `admin`, `create_time`, `update_time`, `staff_id`) VALUES ('admin', 'Alex', '123@qq.com', '/assets/cat.jpg', '001', 'IT', '科技部', NULL, NULL, 1, NULL, NULL, NULL);
INSERT INTO sys_dept (`id`, `dept_id`, `dept_code`, `dept_tw`, `create_time`, `update_time`) VALUES ('1739849371989209090', '001', 'IT', '科技部', '2023-12-27 12:06:40', '2023-12-27 12:06:43');
INSERT INTO t_form_definition (`id`, `name`, `description`, `definition`, `creator`, `create_time`, `update_time`) VALUES ('1727969723257466881', '[IT]账号申请表(测试)', '[IT]账号申请表(测试)', '{\"id\":\"1727969723257466881\",\"name\":\"[IT]账号申请表(测试)\",\"description\":\"[IT]账号申请表(测试)\",\"definition\":null,\"creator\":null,\"createTime\":null,\"updateTime\":null,\"config\":{\"layout\":\"horizontal\"},\"children\":[{\"type\":\"input\",\"noColon\":false,\"label\":\"账号ID\",\"required\":false,\"placeholder\":\"\",\"dyColSpan\":12,\"value\":\"\",\"field\":\"customerId\"},{\"type\":\"row\",\"layout\":\"horizontal\",\"children\":[{\"type\":\"select\",\"label\":\"申请类型\",\"dyColSpan\":12,\"options\":\"2.1 来账-代理行账户开立申请\\n2.2 来账-普通账户开立申请\\n2.3 往账-清算账户开立申请\\n2.4 往账-普通账户开立申请\\n2.5 来账-代理行账户关闭申请\\n2.6 来账-普通账户关闭申请\\n2.7 往账-清算账户关闭申请\\n2.8 往账-普通账户关闭申请\\n2.9 高风险客户普通账户开立、关闭申请\",\"mode\":\"default\",\"value\":\"\",\"snapshot\":false,\"required\":true,\"field\":\"type\",\"msg\":\"必填項\"},{\"type\":\"select\",\"label\":\"客户认定\",\"dyColSpan\":12,\"options\":\"客户\\n非客户\",\"mode\":\"default\",\"value\":\"\",\"snapshot\":false,\"field\":\"custType\",\"required\":true,\"msg\":\"必填项\"}],\"gutter\":24},{\"type\":\"row\",\"layout\":\"horizontal\",\"children\":[{\"type\":\"input\",\"noColon\":false,\"label\":\"公司中文名称\",\"required\":true,\"placeholder\":\"\",\"dyColSpan\":12,\"value\":\"\",\"field\":\"nameCn\",\"msg\":\"请输入公司中文名\",\"regex\":\"^[^\\\\w ]+$\"},{\"type\":\"input\",\"noColon\":false,\"label\":\"公司英文名称\",\"required\":true,\"placeholder\":\"\",\"dyColSpan\":12,\"value\":\"\",\"field\":\"nameEn\",\"msg\":\"请输入公司英文名\",\"regex\":\"^[\\\\w ]+$\"}],\"gutter\":24},{\"type\":\"textarea\",\"noColon\":false,\"label\":\"备注\",\"dyColSpan\":12,\"required\":false,\"placeholder\":\"\",\"height\":\"65px\",\"value\":\"\",\"field\":\"remark\"},{\"type\":\"row\",\"layout\":\"horizontal\",\"children\":[{\"type\":\"select\",\"label\":\"最终风险评级\",\"dyColSpan\":12,\"options\":\"高风险\\n中高风险\\n中风险\\n中低风险\\n低风险\\n无\",\"mode\":\"default\",\"value\":\"\",\"snapshot\":false,\"field\":\"finalRiskLevel\",\"required\":true},{\"type\":\"datepicker\",\"noColon\":false,\"label\":\"下次检查日期\",\"dyColSpan\":12,\"required\":true,\"showTime\":false,\"field\":\"nextAuditDate\",\"tabIndex\":1}],\"gutter\":24},{\"type\":\"select\",\"label\":\"会签部门(多选)\",\"dyColSpan\":12,\"options\":\"001MA01|科技部主管\\n002MA01|人事部主管\\n003MA01|财务部主管\",\"mode\":\"multiple\",\"value\":[],\"snapshot\":false,\"required\":false,\"msg\":\"\",\"field\":\"counterSignDept\",\"allowClear\":true},{\"type\":\"upload\",\"noColon\":false,\"label\":\"附件\",\"name\":\"file\",\"dyColSpan\":12,\"required\":false,\"placeholder\":\"\",\"url\":\"/upload\",\"res\":\"data\",\"value\":[],\"field\":\"attachments\"},{\"type\":\"select\",\"label\":\"复核环节\",\"dyColSpan\":12,\"options\":\"\",\"mode\":\"default\",\"value\":\"\",\"snapshot\":true,\"field\":\"nextHandler\",\"tabIndex\":1,\"url\":\"/api/role-users?page=1&size=1000&roleId=DR${DEPT_ID}\",\"required\":true,\"msg\":\"必填項\",\"resValue\":\"id\",\"res\":\"data.list\",\"resLabel\":\"username\",\"resLabel2\":\"id\",\"resLabel3\":\"email\"},{\"type\":\"select\",\"label\":\"部门副主管\",\"dyColSpan\":12,\"options\":\"\",\"mode\":\"default\",\"value\":\"\",\"snapshot\":true,\"field\":\"viceMaster\",\"required\":true,\"msg\":\"必填項\",\"tabIndex\":1,\"url\":\"/api/role-users?roleId=${DEPT_ID}VMA01\",\"res\":\"data.list\",\"resLabel\":\"username\",\"resLabel2\":\"id\",\"resValue\":\"id\",\"resLabel3\":\"email\"},{\"type\":\"select\",\"label\":\"部门主管\",\"dyColSpan\":12,\"options\":\"\",\"mode\":\"default\",\"value\":\"\",\"snapshot\":true,\"field\":\"master\",\"required\":true,\"msg\":\"必填項\",\"tabIndex\":1,\"url\":\"/api/role-users?roleId=${DEPT_ID}MA01\",\"res\":\"data.list\",\"resLabel\":\"username\",\"resLabel2\":\"id\",\"resValue\":\"id\",\"resLabel3\":\"email\"},{\"type\":\"select\",\"label\":\"分管領導\",\"dyColSpan\":12,\"options\":\"\",\"mode\":\"default\",\"value\":\"\",\"snapshot\":true,\"field\":\"leader\",\"required\":true,\"msg\":\"必填項\",\"tabIndex\":1,\"url\":\"/api/role-users?roleId=leader\",\"res\":\"data.list\",\"resLabel\":\"username\",\"resLabel2\":\"id\",\"resValue\":\"id\",\"resLabel3\":\"email\"},{\"type\":\"select\",\"label\":\"總裁\",\"dyColSpan\":12,\"options\":\"\",\"mode\":\"default\",\"value\":\"\",\"snapshot\":true,\"field\":\"ceo\",\"required\":true,\"msg\":\"必填項\",\"tabIndex\":1,\"url\":\"/api/role-users?roleId=ceo\",\"res\":\"data.list\",\"resLabel\":\"username\",\"resLabel2\":\"id\",\"resValue\":\"id\",\"resLabel3\":\"email\"}]}', '001249011', '2023-11-24 16:37:41', '2023-12-27 13:40:40');
INSERT INTO t_process_definition (`id`, `name`, `status`, `seq_no`, `icon`, `form_id`, `description`, `creator`, `create_time`, `update_time`, `group_name`, `ru_id`, `managers`) VALUES ('1727977674923847681', '賬戶管理申請', 0, '3002', '', '1727969723257466881', '账号管理测试流程', '001249011', '2023-11-24 17:09:17', '2023-12-27 07:54:34', 'IT 科技部', 'caf0dfcc879a08efba043e9988378dc7', 'admin,it3');
INSERT INTO t_process_definition_ru (`id`, `definition_id`, `create_time`, `dag`) VALUES ('caf0dfcc879a08efba043e9988378dc7', '1727977674923847681', '2023-12-27 12:01:48', '[{\"shape\":\"dag-edge\",\"attrs\":{\"line\":{\"strokeDasharray\":\"\"}},\"id\":\"f84f3485-ec3e-4be8-a219-52ceb747fe88\",\"zIndex\":-1,\"source\":{\"cell\":\"7133740538326421504\",\"port\":\"7133740538330615809\"},\"target\":{\"cell\":\"7133740956372701184\",\"port\":\"7133740956372701185\"}},{\"shape\":\"dag-edge\",\"attrs\":{\"line\":{\"strokeDasharray\":\"\"}},\"id\":\"ddd04b0f-0fc6-4471-b2a2-718734050a87\",\"zIndex\":-1,\"source\":{\"cell\":\"7133740956372701184\",\"port\":\"7133740956372701188\"},\"target\":{\"cell\":\"7133741018960105472\",\"port\":\"7133741018960105475\"}},{\"shape\":\"dag-edge\",\"attrs\":{\"line\":{\"strokeDasharray\":\"\"}},\"id\":\"c6d1deb1-023a-474b-9c28-eeeb728d82fd\",\"zIndex\":-1,\"source\":{\"cell\":\"7133741318823481344\",\"port\":\"7133741318823481348\"},\"target\":{\"cell\":\"7133741423244873728\",\"port\":\"7133741423244873732\"}},{\"shape\":\"dag-edge\",\"attrs\":{\"line\":{\"strokeDasharray\":\"\"}},\"id\":\"127a0e7c-4508-4723-aaf8-393191eccc68\",\"zIndex\":-1,\"data\":{\"condition\":\"\",\"label\":\"是否需要会签\"},\"labels\":[{\"attrs\":{\"label\":{\"text\":\"是否需要会签\"}}}],\"source\":{\"cell\":\"7133741119275274240\",\"port\":\"7133741119275274244\"},\"target\":{\"cell\":\"7133741463908651008\",\"port\":\"7133741463908651012\"}},{\"shape\":\"dag-edge\",\"attrs\":{\"line\":{\"strokeDasharray\":\"\"}},\"id\":\"258f2885-3442-4d1c-9276-98b645fe718d\",\"zIndex\":-1,\"data\":{\"condition\":\"\",\"label\":\"是\"},\"labels\":[{\"attrs\":{\"label\":{\"text\":\"是\"}}}],\"source\":{\"cell\":\"7133741463908651008\",\"port\":\"7133741463908651010\"},\"target\":{\"cell\":\"7133741583135936512\",\"port\":\"7133741583135936515\"}},{\"shape\":\"dag-edge\",\"attrs\":{\"line\":{\"strokeDasharray\":\"\"}},\"id\":\"83ccc919-51a2-4d0f-a7ca-4b3d1234dd18\",\"zIndex\":-1,\"data\":{\"condition\":\"counterSignDept==null || counterSignDept.size()==0\",\"label\":\"否\"},\"labels\":[{\"attrs\":{\"label\":{\"text\":\"否\"}}}],\"source\":{\"cell\":\"7133741463908651008\",\"port\":\"7133741463908651011\"},\"target\":{\"cell\":\"7133741318823481344\",\"port\":\"7133741318823481345\"}},{\"shape\":\"dag-edge\",\"attrs\":{\"line\":{\"strokeDasharray\":\"\"}},\"id\":\"4a93b5e8-d662-4b0b-94c3-dda466c5e1a3\",\"zIndex\":-1,\"data\":{\"condition\":\"Stream.of(\\\"2.1\\\",\\\"2.3\\\",\\\"2.5\\\",\\\"2.7\\\",\\\"2.9\\\").anyMatch(m->m==typeId)\",\"label\":\"(2.1/2.3/2.5/2.7/2.9)\"},\"labels\":[{\"attrs\":{\"label\":{\"text\":\"(2.1/2.3/2.5/2.7/2.9)\"}}}],\"source\":{\"cell\":\"7133741423244873728\",\"port\":\"7133741423244873730\"},\"target\":{\"cell\":\"7133741700106686464\",\"port\":\"7133741700106686467\"}},{\"shape\":\"dag-edge\",\"attrs\":{\"line\":{\"strokeDasharray\":\"\"}},\"id\":\"ac38be14-d352-4108-a015-778f8cb3cb31\",\"zIndex\":-1,\"data\":{\"condition\":\"\",\"label\":\"\"},\"labels\":[{\"attrs\":{\"label\":{\"text\":\"\"}}}],\"source\":{\"cell\":\"7133741423244873728\",\"port\":\"7133741423244873731\"},\"target\":{\"cell\":\"7133741965404803072\",\"port\":\"7133741965404803073\"}},{\"shape\":\"dag-edge\",\"attrs\":{\"line\":{\"strokeDasharray\":\"\"}},\"id\":\"cc614bde-742a-4fb9-a2d6-898bc7626f42\",\"zIndex\":-1,\"source\":{\"cell\":\"7133741700106686464\",\"port\":\"7133741700106686466\"},\"target\":{\"cell\":\"7133741965404803072\",\"port\":\"7133741965404803076\"}},{\"shape\":\"dag-edge\",\"attrs\":{\"line\":{\"strokeDasharray\":\"\"}},\"id\":\"cfb1bc39-99c8-431d-a2db-6dc85f49fe75\",\"zIndex\":-1,\"source\":{\"cell\":\"7133741965404803072\",\"port\":\"7133741965404803075\"},\"target\":{\"cell\":\"7133742036963823616\",\"port\":\"7133742036968017923\"}},{\"shape\":\"dag-edge\",\"attrs\":{\"line\":{\"strokeDasharray\":\"\"}},\"id\":\"d91c4346-34f4-458a-b5c9-e7d1dc58c34a\",\"zIndex\":-1,\"source\":{\"cell\":\"7133742036963823616\",\"port\":\"7133742036968017922\"},\"target\":{\"cell\":\"7133742050247184384\",\"port\":\"7133742050247184386\"}},{\"shape\":\"dag-edge\",\"attrs\":{\"line\":{\"strokeDasharray\":\"\"}},\"id\":\"7a89c7c2-59b1-47d9-a2c3-3f0832c2d24d\",\"zIndex\":-1,\"source\":{\"cell\":\"7133741018960105472\",\"port\":\"7133741018960105473\"},\"target\":{\"cell\":\"7138057401067900928\",\"port\":\"7138057401072095233\"}},{\"shape\":\"dag-edge\",\"attrs\":{\"line\":{\"strokeDasharray\":\"\"}},\"id\":\"14227d22-d494-4f30-ae82-2fd307b72935\",\"zIndex\":-1,\"source\":{\"cell\":\"7138057401067900928\",\"port\":\"7138057401072095232\"},\"target\":{\"cell\":\"7133741119275274240\",\"port\":\"7133741119275274242\"}},{\"shape\":\"dag-edge\",\"attrs\":{\"line\":{\"strokeDasharray\":\"\"}},\"id\":\"f1b5e519-0f51-4109-9101-f991f8203186\",\"zIndex\":-1,\"source\":{\"cell\":\"7133741583135936512\",\"port\":\"7133741583135936514\"},\"target\":{\"cell\":\"7138069052546617344\",\"port\":\"7138069052546617345\"}},{\"shape\":\"dag-edge\",\"attrs\":{\"line\":{\"strokeDasharray\":\"\"}},\"id\":\"0f904435-eab1-44ec-a73b-ea5e916b859f\",\"zIndex\":-1,\"source\":{\"cell\":\"7138069052546617344\",\"port\":\"7138069052546617347\"},\"target\":{\"cell\":\"7133741318823481344\",\"port\":\"7133741318823481345\"}},{\"position\":{\"x\":320,\"y\":260},\"size\":{\"width\":40,\"height\":40},\"view\":\"html-view\",\"shape\":\"start-node\",\"ports\":{\"groups\":{\"top\":{\"position\":\"top\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"left\":{\"position\":\"left\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"right\":{\"position\":\"right\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"bottom\":{\"position\":\"bottom\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}}},\"items\":[{\"id\":\"7133740538330615808\",\"group\":\"top\"},{\"id\":\"7133740538330615809\",\"group\":\"bottom\"},{\"id\":\"7133740538330615810\",\"group\":\"left\"},{\"id\":\"7133740538330615811\",\"group\":\"right\"}]},\"id\":\"7133740538326421504\",\"zIndex\":1},{\"position\":{\"x\":250,\"y\":420},\"size\":{\"width\":180,\"height\":40},\"view\":\"html-view\",\"shape\":\"system-node\",\"ports\":{\"groups\":{\"top\":{\"position\":\"top\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"left\":{\"position\":\"left\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"right\":{\"position\":\"right\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"bottom\":{\"position\":\"bottom\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}}},\"items\":[{\"id\":\"7133740956372701185\",\"group\":\"top\"},{\"id\":\"7133740956372701186\",\"group\":\"bottom\"},{\"id\":\"7133740956372701187\",\"group\":\"left\"},{\"id\":\"7133740956372701188\",\"group\":\"right\"}]},\"id\":\"7133740956372701184\",\"data\":{\"label\":\"系统处理\",\"status\":\"default\",\"script\":\"CTX.put(\\\"typeId\\\",type.split(\\\" \\\")[0])\"},\"zIndex\":2},{\"position\":{\"x\":550,\"y\":420},\"size\":{\"width\":180,\"height\":40},\"view\":\"html-view\",\"shape\":\"user-task-node\",\"ports\":{\"groups\":{\"top\":{\"position\":\"top\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"left\":{\"position\":\"left\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"right\":{\"position\":\"right\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"bottom\":{\"position\":\"bottom\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}}},\"items\":[{\"id\":\"7133741018960105473\",\"group\":\"top\"},{\"id\":\"7133741018960105474\",\"group\":\"bottom\"},{\"id\":\"7133741018960105475\",\"group\":\"left\"},{\"id\":\"7133741018960105476\",\"group\":\"right\"}]},\"id\":\"7133741018960105472\",\"data\":{\"label\":\"调查复核\",\"status\":\"default\",\"userList\":[],\"roleList\":[],\"userScript\":\"nextHandler\",\"roleScript\":\"\"},\"zIndex\":3},{\"position\":{\"x\":550,\"y\":160},\"size\":{\"width\":180,\"height\":40},\"view\":\"html-view\",\"shape\":\"user-task-node\",\"ports\":{\"groups\":{\"top\":{\"position\":\"top\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"left\":{\"position\":\"left\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"right\":{\"position\":\"right\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"bottom\":{\"position\":\"bottom\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}}},\"items\":[{\"id\":\"7133741119275274241\",\"group\":\"top\"},{\"id\":\"7133741119275274242\",\"group\":\"bottom\"},{\"id\":\"7133741119275274243\",\"group\":\"left\"},{\"id\":\"7133741119275274244\",\"group\":\"right\"}]},\"id\":\"7133741119275274240\",\"data\":{\"label\":\"部门主管\",\"status\":\"default\",\"userList\":[],\"roleList\":[],\"userScript\":\"master\",\"roleScript\":\"\"},\"zIndex\":4},{\"position\":{\"x\":930,\"y\":420},\"size\":{\"width\":180,\"height\":40},\"view\":\"html-view\",\"shape\":\"user-task-node\",\"ports\":{\"groups\":{\"top\":{\"position\":\"top\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"left\":{\"position\":\"left\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"right\":{\"position\":\"right\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"bottom\":{\"position\":\"bottom\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}}},\"items\":[{\"id\":\"7133741318823481345\",\"group\":\"top\"},{\"id\":\"7133741318823481346\",\"group\":\"bottom\"},{\"id\":\"7133741318823481347\",\"group\":\"left\"},{\"id\":\"7133741318823481348\",\"group\":\"right\"}]},\"id\":\"7133741318823481344\",\"data\":{\"label\":\"部门分管\",\"status\":\"default\",\"userList\":[],\"roleList\":[],\"userScript\":\"leader\",\"roleScript\":\"\"},\"zIndex\":5},{\"position\":{\"x\":1190,\"y\":410},\"size\":{\"width\":60,\"height\":60},\"view\":\"html-view\",\"shape\":\"gateway-node\",\"ports\":{\"groups\":{\"top\":{\"position\":\"top\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"left\":{\"position\":\"left\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"right\":{\"position\":\"right\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"bottom\":{\"position\":\"bottom\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}}},\"items\":[{\"id\":\"7133741423244873729\",\"group\":\"top\"},{\"id\":\"7133741423244873730\",\"group\":\"right\"},{\"id\":\"7133741423244873731\",\"group\":\"bottom\"},{\"id\":\"7133741423244873732\",\"group\":\"left\"}]},\"id\":\"7133741423244873728\",\"data\":{\"label\":\"新建网关\",\"status\":\"default\"},\"zIndex\":6},{\"position\":{\"x\":880,\"y\":150},\"size\":{\"width\":60,\"height\":60},\"view\":\"html-view\",\"shape\":\"gateway-node\",\"ports\":{\"groups\":{\"top\":{\"position\":\"top\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"left\":{\"position\":\"left\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"right\":{\"position\":\"right\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"bottom\":{\"position\":\"bottom\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}}},\"items\":[{\"id\":\"7133741463908651009\",\"group\":\"top\"},{\"id\":\"7133741463908651010\",\"group\":\"right\"},{\"id\":\"7133741463908651011\",\"group\":\"bottom\"},{\"id\":\"7133741463908651012\",\"group\":\"left\"}]},\"id\":\"7133741463908651008\",\"data\":{\"label\":\"新建网关\",\"status\":\"default\"},\"zIndex\":7},{\"position\":{\"x\":1080,\"y\":160},\"size\":{\"width\":180,\"height\":40},\"view\":\"html-view\",\"shape\":\"user-task-node\",\"ports\":{\"groups\":{\"top\":{\"position\":\"top\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"left\":{\"position\":\"left\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"right\":{\"position\":\"right\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"bottom\":{\"position\":\"bottom\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}}},\"items\":[{\"id\":\"7133741583135936513\",\"group\":\"top\"},{\"id\":\"7133741583135936514\",\"group\":\"bottom\"},{\"id\":\"7133741583135936515\",\"group\":\"left\"},{\"id\":\"7133741583135936516\",\"group\":\"right\"}]},\"id\":\"7133741583135936512\",\"data\":{\"label\":\"部门会签\",\"status\":\"default\",\"userList\":[],\"roleList\":[],\"userScript\":\"\",\"roleScript\":\"counterSignDept\",\"countersign\":true},\"zIndex\":8},{\"position\":{\"x\":1440,\"y\":420},\"size\":{\"width\":180,\"height\":40},\"view\":\"html-view\",\"shape\":\"user-task-node\",\"ports\":{\"groups\":{\"top\":{\"position\":\"top\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"left\":{\"position\":\"left\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"right\":{\"position\":\"right\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"bottom\":{\"position\":\"bottom\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}}},\"items\":[{\"id\":\"7133741700106686465\",\"group\":\"top\"},{\"id\":\"7133741700106686466\",\"group\":\"bottom\"},{\"id\":\"7133741700106686467\",\"group\":\"left\"},{\"id\":\"7133741700106686468\",\"group\":\"right\"}]},\"id\":\"7133741700106686464\",\"data\":{\"label\":\"行政总裁\",\"status\":\"default\",\"userList\":[],\"roleList\":[],\"userScript\":\"ceo\",\"roleScript\":\"\"},\"zIndex\":9},{\"position\":{\"x\":1130,\"y\":630},\"size\":{\"width\":180,\"height\":40},\"view\":\"html-view\",\"shape\":\"user-task-node\",\"ports\":{\"groups\":{\"top\":{\"position\":\"top\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"left\":{\"position\":\"left\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"right\":{\"position\":\"right\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"bottom\":{\"position\":\"bottom\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}}},\"items\":[{\"id\":\"7133741965404803073\",\"group\":\"top\"},{\"id\":\"7133741965404803074\",\"group\":\"bottom\"},{\"id\":\"7133741965404803075\",\"group\":\"left\"},{\"id\":\"7133741965404803076\",\"group\":\"right\"}]},\"id\":\"7133741965404803072\",\"data\":{\"label\":\"调查人归档\",\"status\":\"default\",\"userList\":[\"{starter}\"],\"roleList\":[],\"userScript\":\"\",\"roleScript\":\"\"},\"zIndex\":10},{\"position\":{\"x\":810,\"y\":630},\"size\":{\"width\":180,\"height\":40},\"view\":\"html-view\",\"shape\":\"system-node\",\"ports\":{\"groups\":{\"top\":{\"position\":\"top\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"left\":{\"position\":\"left\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"right\":{\"position\":\"right\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"bottom\":{\"position\":\"bottom\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}}},\"items\":[{\"id\":\"7133742036968017920\",\"group\":\"top\"},{\"id\":\"7133742036968017921\",\"group\":\"bottom\"},{\"id\":\"7133742036968017922\",\"group\":\"left\"},{\"id\":\"7133742036968017923\",\"group\":\"right\"}]},\"id\":\"7133742036963823616\",\"data\":{\"label\":\"归档数据\",\"status\":\"default\",\"script\":\"println(\\\"测试打印当前文字\\\")\",\"url\":\"\",\"method\":\"GET\",\"header\":\"\",\"body\":\"\",\"condition\":\"\",\"retry\":1,\"delay\":10000,\"connectTimeout\":3000,\"socketTimeout\":5000},\"zIndex\":11},{\"position\":{\"x\":580,\"y\":630},\"size\":{\"width\":40,\"height\":40},\"view\":\"html-view\",\"shape\":\"end-node\",\"ports\":{\"groups\":{\"top\":{\"position\":\"top\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"left\":{\"position\":\"left\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"right\":{\"position\":\"right\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"bottom\":{\"position\":\"bottom\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}}},\"items\":[{\"id\":\"7133742050247184385\",\"group\":\"top\"},{\"id\":\"7133742050247184386\",\"group\":\"right\"},{\"id\":\"7133742050251378688\",\"group\":\"bottom\"},{\"id\":\"7133742050251378689\",\"group\":\"left\"}]},\"id\":\"7133742050247184384\",\"zIndex\":12},{\"position\":{\"x\":550,\"y\":290},\"size\":{\"width\":180,\"height\":40},\"view\":\"html-view\",\"shape\":\"user-task-node\",\"ports\":{\"groups\":{\"top\":{\"position\":\"top\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"left\":{\"position\":\"left\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"right\":{\"position\":\"right\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"bottom\":{\"position\":\"bottom\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}}},\"items\":[{\"id\":\"7138057401072095232\",\"group\":\"top\"},{\"id\":\"7138057401072095233\",\"group\":\"bottom\"},{\"id\":\"7138057401072095234\",\"group\":\"left\"},{\"id\":\"7138057401072095235\",\"group\":\"right\"}]},\"id\":\"7138057401067900928\",\"data\":{\"label\":\"部門副主管\",\"status\":\"default\",\"userList\":[],\"roleList\":[],\"userScript\":\"viceMaster\",\"roleScript\":\"\",\"formId\":\"\"},\"zIndex\":13},{\"position\":{\"x\":1080,\"y\":290},\"size\":{\"width\":180,\"height\":40},\"view\":\"html-view\",\"shape\":\"user-task-node\",\"ports\":{\"groups\":{\"top\":{\"position\":\"top\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"left\":{\"position\":\"left\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"right\":{\"position\":\"right\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}},\"bottom\":{\"position\":\"bottom\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#C2C8D500\",\"strokeWidth\":1,\"fill\":\"#ffffff00\"}}}},\"items\":[{\"id\":\"7138069052546617345\",\"group\":\"top\"},{\"id\":\"7138069052546617346\",\"group\":\"bottom\"},{\"id\":\"7138069052546617347\",\"group\":\"left\"},{\"id\":\"7138069052550811648\",\"group\":\"right\"}]},\"id\":\"7138069052546617344\",\"data\":{\"label\":\"部门主管\",\"status\":\"default\",\"userList\":[],\"roleList\":[],\"userScript\":\"master\",\"roleScript\":\"\"},\"zIndex\":14}]');

INSERT INTO sys_role (`id`, `name`) VALUES ('001MA01', '科技部主管');
INSERT INTO sys_role (`id`, `name`) VALUES ('001VMA01', '科技部副主管');
INSERT INTO sys_role (`id`, `name`) VALUES ('002MA01', '人力资源部主管');
INSERT INTO sys_role (`id`, `name`) VALUES ('002VMA01', '人力资源部副主管');
INSERT INTO sys_role (`id`, `name`) VALUES ('003MA01', '财务部主管');
INSERT INTO sys_role (`id`, `name`) VALUES ('003VMA01', '财务部副主管');
INSERT INTO sys_role (`id`, `name`) VALUES ('ceo', '行政总裁');
INSERT INTO sys_role (`id`, `name`) VALUES ('DR001', '科技部');
INSERT INTO sys_role (`id`, `name`) VALUES ('DR002', '人事部');
INSERT INTO sys_role (`id`, `name`) VALUES ('DR003', '财务部');
INSERT INTO sys_role (`id`, `name`) VALUES ('leader', '分管领导');

INSERT INTO sys_menu (id, pid, text, icon, link, weight, all_available) VALUES ('1563424145789804546', '0', 'WORK FLOW', null, null, 8000, 1);
INSERT INTO sys_menu (id, pid, text, icon, link, weight, all_available) VALUES ('1563425283947749377', '1563424145789804546', '工作流', 'gateway', null, 8100, 1);
INSERT INTO sys_menu (id, pid, text, icon, link, weight, all_available) VALUES ('1563425482220888065', '1563425283947749377', '流程管理', null, '/process/process-manage', 8200, 1);
INSERT INTO sys_menu (id, pid, text, icon, link, weight, all_available) VALUES ('1642105422302564354', '9100', '部门管理', null, '/system/dept', 9330, 0);
INSERT INTO sys_menu (id, pid, text, icon, link, weight, all_available) VALUES ('1642186485959905282', '1563425283947749377', '表单管理', null, '/form/form-manage', 8400, 0);
INSERT INTO sys_menu (id, pid, text, icon, link, weight, all_available) VALUES ('1642198788675620865', '1563425283947749377', '发起流程', null, '/process/process-list', 8150, 1);
INSERT INTO sys_menu (id, pid, text, icon, link, weight, all_available) VALUES ('1642207119851016193', '1563425283947749377', '我的申请', null, '/process/process-instance', 8120, 1);
INSERT INTO sys_menu (id, pid, text, icon, link, weight, all_available) VALUES ('1642207226415697922', '1563425283947749377', '待办事项', null, '/process/todo-list', 8110, 1);
INSERT INTO sys_menu (id, pid, text, icon, link, weight, all_available) VALUES ('1723948375444471809', '1563425283947749377', '已办事项', null, '/process/checked-list', 8111, 1);
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
INSERT INTO sys_permission (id, scope, name, mark, url, method) VALUES ('1722555006790369282', null, '流程导出', 'pexp', '/api/process-instance-index/download', 'POST');