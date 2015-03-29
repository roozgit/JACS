# --- !Ups

create table blobs (
  id                        bigint auto_increment not null,
  name                      varchar(255),
  tag                       varchar(19),
  creation_date             datetime,
  extension                 varchar(255),
  owner_id                  bigint,
  blob_file                 varchar(255),
  constraint ck_blobs_tag check (tag in ('PID_DRAWING','PFD_DRAWING','PLAN_DRAWING','STRUCTURAL_DRAWING','SINGLE_LINE_DIAGRAM','WIRING_DIAGRAM','GENERAL_DIAGRAM','DATASHEET','OPERATION_MANUAL','SAFETY_MANUAL','GREASING_MANUAL','DOCUMENT','REPORT','WORK_ORDER','FORM','PHOTO')),
  constraint pk_blobs primary key (id))
;

create table companies (
  id                        bigint auto_increment not null,
  name                      varchar(255),
  description               varchar(255),
  company_type              varchar(10),
  country                   varchar(255),
  telephone1                varchar(255),
  telephone2                varchar(255),
  telephone3                varchar(255),
  fax1                      varchar(255),
  fax2                      varchar(255),
  fax3                      varchar(255),
  website                   varchar(255),
  contact_person            varchar(255),
  email                     varchar(255),
  address1                  TEXT,
  address2                  TEXT,
  address3                  TEXT,
  comments                  TEXT,
  company_logo_id           bigint,
  constraint ck_companies_company_type check (company_type in ('VENDOR','CONTRACTOR','GOVERNMENT')),
  constraint pk_companies primary key (id))
;

create table components (
  id                        bigint auto_increment not null,
  subunit_id                bigint,
  component_class_id        bigint,
  name                      varchar(255),
  description               varchar(255),
  component_serial_no       varchar(255),
  manufacturer_company_id   bigint,
  manufacturer_model_designation varchar(255),
  manufacture_date          datetime,
  purchase_date             datetime,
  guarantee                 TEXT,
  guarantee_end_date        datetime,
  comments                  TEXT,
  constraint pk_components primary key (id))
;

create table datasheets (
  id                        bigint auto_increment not null,
  parent_installation_id    bigint,
  parent_plant_id           bigint,
  parent_section_id         bigint,
  parent_equipment_id       bigint,
  parent_subunit_id         bigint,
  parent_component_id       bigint,
  parent_part_id            bigint,
  parent_history_id         bigint,
  parameter                 varchar(255),
  value                     varchar(255),
  min_value                 varchar(255),
  max_value                 varchar(255),
  unit_id                   bigint,
  constraint pk_datasheets primary key (id))
;

create table dimensions (
  id                        bigint auto_increment not null,
  name                      varchar(255),
  comments                  varchar(255),
  constraint pk_dimensions primary key (id))
;

create table disciplines (
  id                        bigint auto_increment not null,
  name                      varchar(255),
  telephone                 varchar(255),
  comments                  TEXT,
  organization_plant_id     bigint,
  head_discipline_id        bigint,
  constraint pk_disciplines primary key (id))
;

create table equipment_category (
  id                        bigint auto_increment not null,
  name                      varchar(255),
  constraint pk_equipment_category primary key (id))
;

create table equipment_class (
  id                        bigint auto_increment not null,
  equipment_category_id     bigint,
  name                      varchar(255),
  name_code                 varchar(255),
  ec_type                   varchar(255),
  ec_type_code              varchar(255),
  ec_type_description       TEXT,
  general_maintenace_method TEXT,
  general_operation_method  TEXT,
  general_maintenace_safety_requirements TEXT,
  general_operation_safety_requirements TEXT,
  general_operator_required_courses TEXT,
  general_maintenance_required_courses TEXT,
  constraint pk_equipment_class primary key (id))
;

create table equipments (
  id                        bigint auto_increment not null,
  equipment_class_id        bigint,
  section_id                bigint,
  name                      varchar(255),
  description               varchar(255),
  equipment_auto_generated_code varchar(255),
  asset_no                  varchar(255),
  equipment_serial_no       varchar(255),
  oreda_code                varchar(255),
  cost_center               varchar(255),
  manufacturer_company_id   bigint,
  manufacturer_model_designation varchar(255),
  manufacture_date          datetime,
  purchase_date             datetime,
  guarantee                 TEXT,
  guarantee_end_date        datetime,
  driven_equipment_id       bigint,
  driver_equipment_id       bigint,
  initial_commissioning_date datetime,
  current_service_start_date datetime,
  current_service_end_date  datetime,
  initial_up_time           float,
  initial_down_time         float,
  operation_mode            varchar(13),
  criticality               varchar(13),
  up_period_hours           float,
  off_period_hours          float,
  operation_plan_start_date datetime,
  redundancy_ratio          float,
  maintenace_method         TEXT,
  maintenace_safety_requirements TEXT,
  operation_method          TEXT,
  operation_safety_requirements TEXT,
  hse_risks                 TEXT,
  safety_risks              TEXT,
  comments                  TEXT,
  constraint ck_equipments_operation_mode check (operation_mode in ('CONTINUOUS','HOT_STAND_BY','COLD_STAND_BY','CYCLIC')),
  constraint ck_equipments_criticality check (criticality in ('MOST_CRITICAL','CRITICAL','LESS_CRITICAL','NOT_CRITICAL')),
  constraint pk_equipments primary key (id))
;

create table failure_causes (
  id                        bigint auto_increment not null,
  code_number               integer,
  notation                  varchar(255),
  subdivision_code_number   integer,
  subdivision_notation      varchar(255),
  description               varchar(255),
  constraint pk_failure_causes primary key (id))
;

create table failure_mechanisms (
  id                        bigint auto_increment not null,
  code_number               integer,
  notation                  varchar(255),
  subdivision_code_number   integer,
  subdivision_notation      varchar(255),
  description               varchar(255),
  constraint pk_failure_mechanisms primary key (id))
;

create table failure_modes (
  id                        bigint auto_increment not null,
  type_number               integer,
  failure_mode_code         varchar(3),
  description               varchar(255),
  constraint pk_failure_modes primary key (id))
;

create table failures (
  id                        bigint auto_increment not null,
  parent_failure_id         bigint,
  severity                  varchar(25),
  function_impact           varchar(7),
  operation_impact          varchar(7),
  safety_impact             varchar(7),
  failure_mode_id           bigint,
  failure_mechanism_id      bigint,
  detection_method          varchar(31),
  comments                  TEXT,
  constraint ck_failures_severity check (severity in ('NONE','VERY_MINOR','MINOR','VERY_LOW','LOW','MODERATE','HIGH','VERY_HIGH','HAZARDOUS_WITH_WARNING','HAZARDOUS_WITHOUT_WARNING')),
  constraint ck_failures_function_impact check (function_impact in ('ZERO','PARTIAL','TOTAL')),
  constraint ck_failures_operation_impact check (operation_impact in ('ZERO','PARTIAL','TOTAL')),
  constraint ck_failures_safety_impact check (safety_impact in ('ZERO','PARTIAL','TOTAL')),
  constraint ck_failures_detection_method check (detection_method in ('PERIODIC_MAINTENANCE','FUNCTIONAL_TESTING','INSPECTION','PERIODIC_CONDITION_MONITORING','CONTINUOUS_CONDITION_MONITORING','PRODUCTION_INTERFERENCE','CASUAL_OBSERVATION','CORRECTIVE_MAINTENANCE','ON_DEMAND','OTHER')),
  constraint pk_failures primary key (id))
;

create table hiring_types (
  id                        bigint auto_increment not null,
  hiring_type               varchar(255),
  constraint pk_hiring_types primary key (id))
;

create table history (
  id                        bigint auto_increment not null,
  parent_installation_id    bigint,
  parent_plant_id           bigint,
  parent_section_id         bigint,
  parent_equipment_id       bigint,
  parent_subunit_id         bigint,
  parent_component_id       bigint,
  start                     datetime,
  end                       datetime,
  ends_same_day             tinyint(1) default 0,
  all_day                   tinyint(1) default 0,
  event_type                varchar(16),
  state                     varchar(14),
  maint_id                  bigint,
  fail_id                   bigint,
  comments                  TEXT,
  system_event              tinyint(1) default 0,
  is_happened               tinyint(1) default 0,
  registrar_id              bigint,
  constraint ck_history_event_type check (event_type in ('NORMAL_OPERATION','MAINTENANCE','FAILURE')),
  constraint ck_history_state check (state in ('RUNNING','STOPPED','FAILED','HOT_STANDBY','COLD_STANDBY','START_UP','IDLE','TESTING','OUT_OF_SERVICE')),
  constraint uq_history_1 unique (maint_id,is_happened),
  constraint uq_history_2 unique (fail_id),
  constraint pk_history primary key (id))
;

create table holidays (
  id                        bigint auto_increment not null,
  holid_date                datetime,
  constraint pk_holidays primary key (id))
;

create table installations (
  id                        bigint auto_increment not null,
  name                      varchar(255),
  description               varchar(255),
  soil_type                 varchar(255),
  soil_corrosiveness        varchar(8),
  earthquake_zone           varchar(255),
  min_humidity              float,
  max_humidity              float,
  min_temperature           float,
  max_temperature           float,
  comments                  TEXT,
  constraint ck_installations_soil_corrosiveness check (soil_corrosiveness in ('SEVERE','MODERATE','BENIGN')),
  constraint uq_installations_name unique (name),
  constraint pk_installations primary key (id))
;

create table inventory_events (
  id                        bigint auto_increment not null,
  name                      varchar(255),
  other_language_name       varchar(255),
  comments                  varchar(255),
  constraint pk_inventory_events primary key (id))
;

create table maintenance_groups (
  id                        bigint auto_increment not null,
  name                      varchar(255),
  percent_complete          float,
  comments                  TEXT,
  constraint uq_maintenance_groups_name unique (name),
  constraint pk_maintenance_groups primary key (id))
;

create table workflow (
  id                        bigint auto_increment not null,
  maintenance_id            bigint,
  user_id                   bigint,
  workflow_stage_id         bigint,
  accept_reject             tinyint(1) default 0,
  reject_reason             varchar(255),
  action_date               datetime,
  constraint pk_workflow primary key (id))
;

create table maintenances (
  id                        bigint auto_increment not null,
  work_request_serial       varchar(255),
  work_order_serial         varchar(255),
  request_date              datetime,
  maintained_equipment_id   bigint,
  maintained_section_id     bigint,
  pm_routine_id             bigint,
  class_level_pmroutine_id  bigint,
  maintenance_group_id      bigint,
  maintenance_priority      integer,
  work_order_description    TEXT,
  responsible_discipline_id bigint,
  workflow_stage_id         bigint,
  maintenance_status        varchar(11),
  hold_reason               varchar(17),
  hold_reason_comment       varchar(255),
  maintenance_category      varchar(22),
  planning_comment          TEXT,
  responsible_person_id     bigint,
  maintenance_activity      varchar(11),
  calculated_total_man_hours float,
  time_to_repair            float,
  calculated_total_time_to_repair float,
  maintenance_report        TEXT,
  constraint ck_maintenances_maintenance_status check (maintenance_status in ('IN_PROGRESS','HOLD','CANCEL','FINISHED')),
  constraint ck_maintenances_hold_reason check (hold_reason in ('SHUTDOWN','OTHER_MAINTENANCE','PART','TOOL','VENDOR','OTHER')),
  constraint ck_maintenances_maintenance_category check (maintenance_category in ('CORRECTIVE_MAINTENANCE','PREVENTIVE_MAINTENANCE','PREDICTIVE_MAINTENANCE','PRESERVATION','MODIFICATION','OTHER')),
  constraint ck_maintenances_maintenance_activity check (maintenance_activity in ('REPLACE','REPAIR','MODIFY','ADJUST','REFIT','CHECK','SERVICE','TEST','INSPECTION','OVERHAUL','COMBINATION','OTHER')),
  constraint uq_maintenances_work_request_serial unique (work_request_serial),
  constraint uq_maintenances_work_order_serial unique (work_order_serial),
  constraint pk_maintenances primary key (id))
;

create table maintenances_components (
  id                        bigint auto_increment not null,
  maintenance_id            bigint,
  component_id              bigint,
  maintenance_activity      varchar(11),
  time_to_repair            float,
  constraint ck_maintenances_components_maintenance_activity check (maintenance_activity in ('REPLACE','REPAIR','MODIFY','ADJUST','REFIT','CHECK','SERVICE','TEST','INSPECTION','OVERHAUL','COMBINATION','OTHER')),
  constraint uq_maintenances_components_1 unique (maintenance_id,component_id),
  constraint pk_maintenances_components primary key (id))
;

create table maintenances_parts (
  id                        bigint auto_increment not null,
  maintenance_id            bigint,
  part_id                   bigint,
  quantity                  float,
  stock_flag                tinyint(1) default 0,
  constraint uq_maintenances_parts_1 unique (maintenance_id,part_id),
  constraint pk_maintenances_parts primary key (id))
;

create table maintenances_subunits (
  id                        bigint auto_increment not null,
  maintenance_id            bigint,
  subunit_id                bigint,
  maintenance_activity      varchar(11),
  time_to_repair            float,
  constraint ck_maintenances_subunits_maintenance_activity check (maintenance_activity in ('REPLACE','REPAIR','MODIFY','ADJUST','REFIT','CHECK','SERVICE','TEST','INSPECTION','OVERHAUL','COMBINATION','OTHER')),
  constraint uq_maintenances_subunits_1 unique (maintenance_id,subunit_id),
  constraint pk_maintenances_subunits primary key (id))
;

create table maintenances_users (
  id                        bigint auto_increment not null,
  maintenance_id            bigint,
  user_id                   bigint,
  hours                     float,
  constraint uq_maintenances_users_1 unique (maintenance_id,user_id),
  constraint pk_maintenances_users primary key (id))
;

create table measurement_units (
  id                        bigint auto_increment not null,
  dimension_id              bigint,
  system                    varchar(255),
  unit_name                 varchar(255),
  unit_name2                varchar(255),
  unit_code                 varchar(255),
  comments                  varchar(255),
  constraint pk_measurement_units primary key (id))
;

create table part_history (
  id                        bigint auto_increment not null,
  parent_part_id            bigint,
  parent_maintenance_id     bigint,
  receipt_number            varchar(255),
  request_number            varchar(255),
  commence_date             datetime,
  event_type_id             bigint,
  stock_balance             float,
  remaining_stock           float,
  offered_unit_price        float,
  requester_id              bigint,
  registrar_id              bigint,
  attached_doc_id           bigint,
  comments                  TEXT,
  constraint pk_part_history primary key (id))
;

create table parts (
  id                        bigint auto_increment not null,
  name                      varchar(255),
  description               varchar(255),
  original_vendor_id        bigint,
  mesc_code                 varchar(255),
  vendor_code               varchar(255),
  plant_code                varchar(255),
  asset_code                varchar(255),
  user_code                 varchar(255),
  vendor_drawing_no         varchar(255),
  material1                 varchar(255),
  material2                 varchar(255),
  material3                 varchar(255),
  storage_location          varchar(255),
  unit_price                float,
  minimum_required          float,
  maximum_required          float,
  currency                  varchar(3),
  remaining_quantity        float,
  measurement_unit_id       bigint,
  comments                  TEXT,
  constraint ck_parts_currency check (currency in ('USD','EUR','IRR')),
  constraint pk_parts primary key (id))
;

create table parts_components (
  id                        bigint auto_increment not null,
  component_id              bigint,
  part_id                   bigint,
  quantity                  float,
  constraint uq_parts_components_1 unique (component_id,part_id),
  constraint pk_parts_components primary key (id))
;

create table plants (
  id                        bigint auto_increment not null,
  name                      varchar(255),
  description               varchar(255),
  comments                  TEXT,
  installation_id           bigint,
  constraint uq_plants_name unique (name),
  constraint pk_plants primary key (id))
;

create table preventive_maitenances (
  id                        bigint auto_increment not null,
  parent_plant_id           bigint,
  parent_section_id         bigint,
  parent_equipment_id       bigint,
  name                      varchar(255),
  description               TEXT,
  safety_requirements       TEXT,
  acting_discipline_id      bigint,
  interval_days             float,
  interval_operation_hours  float,
  on_shut_down              tinyint(1) default 0,
  pm_class_id               bigint,
  constraint pk_preventive_maitenances primary key (id))
;

create table repairtools (
  id                        bigint auto_increment not null,
  name                      varchar(255),
  manufacturer_company_id   bigint,
  manufacturer_model_designation varchar(255),
  serial_no                 varchar(255),
  owner_id                  bigint,
  comments                  TEXT,
  constraint pk_repairtools primary key (id))
;

create table sections (
  id                        bigint auto_increment not null,
  name                      varchar(255),
  description               varchar(255),
  section_category          varchar(30),
  comments                  TEXT,
  plant_id                  bigint,
  constraint ck_sections_section_category check (section_category in ('ALARM_SYSTEM','BLOW_DOWN_AND_RELIEF_SYSTEM','BOIL_OFF_GAS_RECOVERY','CHEMICAL_INJECTION','CO2_H2S_REMOVAL','COMPRESSED_AIR','COOLING_SYSTEM','COOLING_WATER','DEHYDRATION','EMERGENCY_POWER','EMERGENCY_PREPAREDNESS_SYSTEMS','EMERGENCY_PROCESS_SHUTDOWN','ESSENTIAL_POWER','FIRE_AND_GAS_DETECTION','FIRE_FIGHTING_SYSTEMS','FIRE_WATER_SYSTEMS','FISCAL_METERING','FLARE_SYSTEM','FRACTIONATION','FRESH_WATER_SYSTEMS','FUEL_GAS','GAS_EXPORT_SYSTEMS','GAS_PROCESS_TREATMENT','HEATING_SYSTEM','HVAC','INSTRUMENT_AIR','LIQUEFACTION','LNG_LOADING_UNLOADING','LNG_STORAGE','MAIN_POWER','MATERIALS_HANDLING','MERCURY_REMOVAL','METHANOL','NITROGEN','OIL_CONDENSATE_EXPORT_SYSTEMS','OIL_PROCESS_TREATMENT','OILY_WATER_TREATMENT','PROCESS_CONTROL','RECONDENSING','REFRIGERANT_STORAGE','REFRIGERATION','STEAM','VAPORIZERS','WATER_INJECTION','WATER_PROCESS_TREATMENT')),
  constraint pk_sections primary key (id))
;

create table security_role (
  id                        bigint auto_increment not null,
  name                      varchar(255),
  description               varchar(255),
  constraint pk_security_role primary key (id))
;

create table shifts (
  id                        bigint auto_increment not null,
  sequence                  bigint,
  user_id                   bigint,
  start                     datetime,
  end                       datetime,
  ends_same_day             tinyint(1) default 0,
  all_day                   tinyint(1) default 0,
  work_type_id              bigint,
  constraint pk_shifts primary key (id))
;

create table subunits (
  id                        bigint auto_increment not null,
  equipment_id              bigint,
  name                      varchar(255),
  description               varchar(255),
  subunit_type              varchar(27),
  comments                  TEXT,
  constraint ck_subunits_subunit_type check (subunit_type in ('ACCESSORY_DRIVE','ACTUATOR','AIR_INTAKE','ANALOG_INPUT_CARDS','ANALOG_OUTPUT_CARDS','BATTERY_UNIT','BOOM','BYPASS_UNIT','COLUMN','COMBUSTION','COMPRESSOR_TRAIN','CONDENSER','CONDITIONING_ELECTRONIC','CONTROL_MONITORING','COOLING_SYSTEM','CRANE_POWER','CRANE_STRUCTURE','DIGITAL_INPUT_CARDS','DIGITAL_OUTPUT_CARDS','ELECTRIC_GENERATOR','ELECTRIC_MOTOR','ENGINE','EXHAUST','EXPANDER_TURBINE','EXTERNALS','FIRE_GAS_PROTECTION','FUEL_GAS','HOIST','HP_TURBINE','INTERFACE_UNIT','INTERNALS','INVERTER_UNIT','LOGIC_SOLVER','LP_TURBINE','LUBRICATION','MISCELLANEOUS','MOORING','NOZZLE_MOUNTING_ASSEMBLY','NOZZLE','PIPE','POWER_SUPPLY','POWER_TRANSMISSION','PUMP_UNIT','RECTIFIER_UNIT','REGULATING_SYSTEM','RISER_UMBILICAL_TERMINATION','SENSOR','SHAFT_SEAL','STARTING','SWING','SWIVEL','SYSTEM_BUS','TRANSFORMER_UNIT','TURRET_UTILITIES','TURRET','VALVE','WATER_INJECTION','WINCH')),
  constraint pk_subunits primary key (id))
;

create table user_blobs (
  id                        bigint auto_increment not null,
  name                      varchar(255),
  tag                       varchar(15),
  creation_date             datetime,
  extension                 varchar(255),
  owner_id                  bigint,
  blob_file                 longblob,
  constraint ck_user_blobs_tag check (tag in ('PERSONNEL_PHOTO','CERTIFICATE','DOCUMENT')),
  constraint pk_user_blobs primary key (id))
;

create table user_permission (
  id                        bigint auto_increment not null,
  permission_value          varchar(255),
  constraint pk_user_permission primary key (id))
;

create table users (
  id                        bigint auto_increment not null,
  user_name                 varchar(255),
  password                  varchar(255),
  discipline_id             bigint,
  first_name                varchar(255),
  last_name                 varchar(255),
  personnel_number          varchar(255),
  home_city                 varchar(255),
  residence_city            varchar(255),
  address                   varchar(255),
  mobile_phone              varchar(255),
  home_phone                varchar(255),
  mobile_phone2             varchar(255),
  home_phone2               varchar(255),
  email                     varchar(255),
  flight_origin             varchar(255),
  camp                      varchar(255),
  suite                     varchar(255),
  suite_phone_number        varchar(255),
  organizational_grade      integer,
  organizational_post       varchar(255),
  hiring_company_id         bigint,
  hiring_type_id            bigint,
  skill                     varchar(12),
  is_on_shift               tinyint(1) default 0,
  comments                  TEXT,
  constraint ck_users_skill check (skill in ('HIGH_SKILL','MEDIUM_SKILL','LOW_SKILL')),
  constraint uq_users_user_name unique (user_name),
  constraint pk_users primary key (id))
;

create table work_types (
  id                        bigint auto_increment not null,
  work_type                 varchar(255),
  work_value                varchar(255),
  constraint pk_work_types primary key (id))
;

create table workflow_tree (
  id                        bigint auto_increment not null,
  tree_category             varchar(22),
  deciding_role_id          bigint,
  referring_role_id         bigint,
  receiving_role_id         bigint,
  description               varchar(255),
  constraint ck_workflow_tree_tree_category check (tree_category in ('CORRECTIVE_MAINTENANCE','PREVENTIVE_MAINTENANCE','PREDICTIVE_MAINTENANCE','PRESERVATION','MODIFICATION','OTHER')),
  constraint pk_workflow_tree primary key (id))
;


create table companies_companies (
  head_companies_id              bigint not null,
  rep_companies_id               bigint not null,
  constraint pk_companies_companies primary key (head_companies_id, rep_companies_id))
;

create table components_blobs (
  components_id                  bigint not null,
  blobs_id                       bigint not null,
  constraint pk_components_blobs primary key (components_id, blobs_id))
;

create table equipment_class_failure_modes (
  equipment_class_id             bigint not null,
  failure_modes_id               bigint not null,
  constraint pk_equipment_class_failure_modes primary key (equipment_class_id, failure_modes_id))
;

create table equipments_repairtools (
  equipments_id                  bigint not null,
  repairtools_id                 bigint not null,
  constraint pk_equipments_repairtools primary key (equipments_id, repairtools_id))
;

create table equipments_blobs (
  equipments_id                  bigint not null,
  blobs_id                       bigint not null,
  constraint pk_equipments_blobs primary key (equipments_id, blobs_id))
;

create table failures_failure_causes (
  failures_id                    bigint not null,
  failure_causes_id              bigint not null,
  constraint pk_failures_failure_causes primary key (failures_id, failure_causes_id))
;

create table installations_blobs (
  installations_id               bigint not null,
  blobs_id                       bigint not null,
  constraint pk_installations_blobs primary key (installations_id, blobs_id))
;

create table maintenances_maintenances (
  prereq_maintenances_id         bigint not null,
  dependent_maintenances_id      bigint not null,
  constraint pk_maintenances_maintenances primary key (prereq_maintenances_id, dependent_maintenances_id))
;

create table maintenances_failures (
  maintenances_id                bigint not null,
  failures_id                    bigint not null,
  constraint pk_maintenances_failures primary key (maintenances_id, failures_id))
;

create table maintenances_equipments (
  maintenances_id                bigint not null,
  equipments_id                  bigint not null,
  constraint pk_maintenances_equipments primary key (maintenances_id, equipments_id))
;

create table maintenances_repairtools (
  maintenances_id                bigint not null,
  repairtools_id                 bigint not null,
  constraint pk_maintenances_repairtools primary key (maintenances_id, repairtools_id))
;

create table maintenances_blobs (
  maintenances_id                bigint not null,
  blobs_id                       bigint not null,
  constraint pk_maintenances_blobs primary key (maintenances_id, blobs_id))
;

create table parts_companies (
  parts_id                       bigint not null,
  companies_id                   bigint not null,
  constraint pk_parts_companies primary key (parts_id, companies_id))
;

create table parts_blobs (
  parts_id                       bigint not null,
  blobs_id                       bigint not null,
  constraint pk_parts_blobs primary key (parts_id, blobs_id))
;

create table plants_blobs (
  plants_id                      bigint not null,
  blobs_id                       bigint not null,
  constraint pk_plants_blobs primary key (plants_id, blobs_id))
;

create table repairtools_blobs (
  repairtools_id                 bigint not null,
  blobs_id                       bigint not null,
  constraint pk_repairtools_blobs primary key (repairtools_id, blobs_id))
;

create table sections_blobs (
  sections_id                    bigint not null,
  blobs_id                       bigint not null,
  constraint pk_sections_blobs primary key (sections_id, blobs_id))
;

create table subunits_blobs (
  subunits_id                    bigint not null,
  blobs_id                       bigint not null,
  constraint pk_subunits_blobs primary key (subunits_id, blobs_id))
;

create table users_user_blobs (
  users_id                       bigint not null,
  user_blobs_id                  bigint not null,
  constraint pk_users_user_blobs primary key (users_id, user_blobs_id))
;

create table users_security_role (
  users_id                       bigint not null,
  security_role_id               bigint not null,
  constraint pk_users_security_role primary key (users_id, security_role_id))
;

create table users_user_permission (
  users_id                       bigint not null,
  user_permission_id             bigint not null,
  constraint pk_users_user_permission primary key (users_id, user_permission_id))
;
alter table blobs add constraint fk_blobs_owner_1 foreign key (owner_id) references users (id) on delete restrict on update restrict;
create index ix_blobs_owner_1 on blobs (owner_id);
alter table companies add constraint fk_companies_companyLogo_2 foreign key (company_logo_id) references blobs (id) on delete restrict on update restrict;
create index ix_companies_companyLogo_2 on companies (company_logo_id);
alter table components add constraint fk_components_subunit_3 foreign key (subunit_id) references subunits (id) on delete restrict on update restrict;
create index ix_components_subunit_3 on components (subunit_id);
alter table components add constraint fk_components_componentClass_4 foreign key (component_class_id) references equipment_class (id) on delete restrict on update restrict;
create index ix_components_componentClass_4 on components (component_class_id);
alter table components add constraint fk_components_manufacturerCompany_5 foreign key (manufacturer_company_id) references companies (id) on delete restrict on update restrict;
create index ix_components_manufacturerCompany_5 on components (manufacturer_company_id);
alter table datasheets add constraint fk_datasheets_parentInstallation_6 foreign key (parent_installation_id) references installations (id) on delete restrict on update restrict;
create index ix_datasheets_parentInstallation_6 on datasheets (parent_installation_id);
alter table datasheets add constraint fk_datasheets_parentPlant_7 foreign key (parent_plant_id) references plants (id) on delete restrict on update restrict;
create index ix_datasheets_parentPlant_7 on datasheets (parent_plant_id);
alter table datasheets add constraint fk_datasheets_parentSection_8 foreign key (parent_section_id) references sections (id) on delete restrict on update restrict;
create index ix_datasheets_parentSection_8 on datasheets (parent_section_id);
alter table datasheets add constraint fk_datasheets_parentEquipment_9 foreign key (parent_equipment_id) references equipments (id) on delete restrict on update restrict;
create index ix_datasheets_parentEquipment_9 on datasheets (parent_equipment_id);
alter table datasheets add constraint fk_datasheets_parentSubunit_10 foreign key (parent_subunit_id) references subunits (id) on delete restrict on update restrict;
create index ix_datasheets_parentSubunit_10 on datasheets (parent_subunit_id);
alter table datasheets add constraint fk_datasheets_parentComponent_11 foreign key (parent_component_id) references components (id) on delete restrict on update restrict;
create index ix_datasheets_parentComponent_11 on datasheets (parent_component_id);
alter table datasheets add constraint fk_datasheets_parentPart_12 foreign key (parent_part_id) references parts (id) on delete restrict on update restrict;
create index ix_datasheets_parentPart_12 on datasheets (parent_part_id);
alter table datasheets add constraint fk_datasheets_parentHistory_13 foreign key (parent_history_id) references history (id) on delete restrict on update restrict;
create index ix_datasheets_parentHistory_13 on datasheets (parent_history_id);
alter table datasheets add constraint fk_datasheets_unit_14 foreign key (unit_id) references measurement_units (id) on delete restrict on update restrict;
create index ix_datasheets_unit_14 on datasheets (unit_id);
alter table disciplines add constraint fk_disciplines_organizationPlant_15 foreign key (organization_plant_id) references plants (id) on delete restrict on update restrict;
create index ix_disciplines_organizationPlant_15 on disciplines (organization_plant_id);
alter table disciplines add constraint fk_disciplines_headDiscipline_16 foreign key (head_discipline_id) references disciplines (id) on delete restrict on update restrict;
create index ix_disciplines_headDiscipline_16 on disciplines (head_discipline_id);
alter table equipment_class add constraint fk_equipment_class_equipmentCategory_17 foreign key (equipment_category_id) references equipment_category (id) on delete restrict on update restrict;
create index ix_equipment_class_equipmentCategory_17 on equipment_class (equipment_category_id);
alter table equipments add constraint fk_equipments_equipmentClass_18 foreign key (equipment_class_id) references equipment_class (id) on delete restrict on update restrict;
create index ix_equipments_equipmentClass_18 on equipments (equipment_class_id);
alter table equipments add constraint fk_equipments_section_19 foreign key (section_id) references sections (id) on delete restrict on update restrict;
create index ix_equipments_section_19 on equipments (section_id);
alter table equipments add constraint fk_equipments_manufacturerCompany_20 foreign key (manufacturer_company_id) references companies (id) on delete restrict on update restrict;
create index ix_equipments_manufacturerCompany_20 on equipments (manufacturer_company_id);
alter table equipments add constraint fk_equipments_drivenEquipment_21 foreign key (driven_equipment_id) references equipments (id) on delete restrict on update restrict;
create index ix_equipments_drivenEquipment_21 on equipments (driven_equipment_id);
alter table equipments add constraint fk_equipments_driverEquipment_22 foreign key (driver_equipment_id) references equipments (id) on delete restrict on update restrict;
create index ix_equipments_driverEquipment_22 on equipments (driver_equipment_id);
alter table failures add constraint fk_failures_parentFailure_23 foreign key (parent_failure_id) references failures (id) on delete restrict on update restrict;
create index ix_failures_parentFailure_23 on failures (parent_failure_id);
alter table failures add constraint fk_failures_failureMode_24 foreign key (failure_mode_id) references failure_modes (id) on delete restrict on update restrict;
create index ix_failures_failureMode_24 on failures (failure_mode_id);
alter table failures add constraint fk_failures_failureMechanism_25 foreign key (failure_mechanism_id) references failure_mechanisms (id) on delete restrict on update restrict;
create index ix_failures_failureMechanism_25 on failures (failure_mechanism_id);
alter table history add constraint fk_history_parentInstallation_26 foreign key (parent_installation_id) references installations (id) on delete restrict on update restrict;
create index ix_history_parentInstallation_26 on history (parent_installation_id);
alter table history add constraint fk_history_parentPlant_27 foreign key (parent_plant_id) references plants (id) on delete restrict on update restrict;
create index ix_history_parentPlant_27 on history (parent_plant_id);
alter table history add constraint fk_history_parentSection_28 foreign key (parent_section_id) references sections (id) on delete restrict on update restrict;
create index ix_history_parentSection_28 on history (parent_section_id);
alter table history add constraint fk_history_parentEquipment_29 foreign key (parent_equipment_id) references equipments (id) on delete restrict on update restrict;
create index ix_history_parentEquipment_29 on history (parent_equipment_id);
alter table history add constraint fk_history_parentSubunit_30 foreign key (parent_subunit_id) references subunits (id) on delete restrict on update restrict;
create index ix_history_parentSubunit_30 on history (parent_subunit_id);
alter table history add constraint fk_history_parentComponent_31 foreign key (parent_component_id) references components (id) on delete restrict on update restrict;
create index ix_history_parentComponent_31 on history (parent_component_id);
alter table history add constraint fk_history_maint_32 foreign key (maint_id) references maintenances (id) on delete restrict on update restrict;
create index ix_history_maint_32 on history (maint_id);
alter table history add constraint fk_history_fail_33 foreign key (fail_id) references failures (id) on delete restrict on update restrict;
create index ix_history_fail_33 on history (fail_id);
alter table history add constraint fk_history_registrar_34 foreign key (registrar_id) references users (id) on delete restrict on update restrict;
create index ix_history_registrar_34 on history (registrar_id);
alter table workflow add constraint fk_workflow_maintenance_35 foreign key (maintenance_id) references maintenances (id) on delete restrict on update restrict;
create index ix_workflow_maintenance_35 on workflow (maintenance_id);
alter table workflow add constraint fk_workflow_user_36 foreign key (user_id) references users (id) on delete restrict on update restrict;
create index ix_workflow_user_36 on workflow (user_id);
alter table workflow add constraint fk_workflow_workflowStage_37 foreign key (workflow_stage_id) references workflow_tree (id) on delete restrict on update restrict;
create index ix_workflow_workflowStage_37 on workflow (workflow_stage_id);
alter table maintenances add constraint fk_maintenances_maintainedEquipment_38 foreign key (maintained_equipment_id) references equipments (id) on delete restrict on update restrict;
create index ix_maintenances_maintainedEquipment_38 on maintenances (maintained_equipment_id);
alter table maintenances add constraint fk_maintenances_maintainedSection_39 foreign key (maintained_section_id) references sections (id) on delete restrict on update restrict;
create index ix_maintenances_maintainedSection_39 on maintenances (maintained_section_id);
alter table maintenances add constraint fk_maintenances_pmRoutine_40 foreign key (pm_routine_id) references preventive_maitenances (id) on delete restrict on update restrict;
create index ix_maintenances_pmRoutine_40 on maintenances (pm_routine_id);
alter table maintenances add constraint fk_maintenances_classLevelPMRoutine_41 foreign key (class_level_pmroutine_id) references preventive_maitenances (id) on delete restrict on update restrict;
create index ix_maintenances_classLevelPMRoutine_41 on maintenances (class_level_pmroutine_id);
alter table maintenances add constraint fk_maintenances_maintenanceGroup_42 foreign key (maintenance_group_id) references maintenance_groups (id) on delete restrict on update restrict;
create index ix_maintenances_maintenanceGroup_42 on maintenances (maintenance_group_id);
alter table maintenances add constraint fk_maintenances_responsibleDiscipline_43 foreign key (responsible_discipline_id) references disciplines (id) on delete restrict on update restrict;
create index ix_maintenances_responsibleDiscipline_43 on maintenances (responsible_discipline_id);
alter table maintenances add constraint fk_maintenances_workflowStage_44 foreign key (workflow_stage_id) references workflow_tree (id) on delete restrict on update restrict;
create index ix_maintenances_workflowStage_44 on maintenances (workflow_stage_id);
alter table maintenances add constraint fk_maintenances_responsiblePerson_45 foreign key (responsible_person_id) references users (id) on delete restrict on update restrict;
create index ix_maintenances_responsiblePerson_45 on maintenances (responsible_person_id);
alter table maintenances_components add constraint fk_maintenances_components_maintenance_46 foreign key (maintenance_id) references maintenances (id) on delete restrict on update restrict;
create index ix_maintenances_components_maintenance_46 on maintenances_components (maintenance_id);
alter table maintenances_components add constraint fk_maintenances_components_component_47 foreign key (component_id) references components (id) on delete restrict on update restrict;
create index ix_maintenances_components_component_47 on maintenances_components (component_id);
alter table maintenances_parts add constraint fk_maintenances_parts_maintenance_48 foreign key (maintenance_id) references maintenances (id) on delete restrict on update restrict;
create index ix_maintenances_parts_maintenance_48 on maintenances_parts (maintenance_id);
alter table maintenances_parts add constraint fk_maintenances_parts_part_49 foreign key (part_id) references parts (id) on delete restrict on update restrict;
create index ix_maintenances_parts_part_49 on maintenances_parts (part_id);
alter table maintenances_subunits add constraint fk_maintenances_subunits_maintenance_50 foreign key (maintenance_id) references maintenances (id) on delete restrict on update restrict;
create index ix_maintenances_subunits_maintenance_50 on maintenances_subunits (maintenance_id);
alter table maintenances_subunits add constraint fk_maintenances_subunits_subunit_51 foreign key (subunit_id) references subunits (id) on delete restrict on update restrict;
create index ix_maintenances_subunits_subunit_51 on maintenances_subunits (subunit_id);
alter table maintenances_users add constraint fk_maintenances_users_maintenance_52 foreign key (maintenance_id) references maintenances (id) on delete restrict on update restrict;
create index ix_maintenances_users_maintenance_52 on maintenances_users (maintenance_id);
alter table maintenances_users add constraint fk_maintenances_users_user_53 foreign key (user_id) references users (id) on delete restrict on update restrict;
create index ix_maintenances_users_user_53 on maintenances_users (user_id);
alter table measurement_units add constraint fk_measurement_units_dimension_54 foreign key (dimension_id) references dimensions (id) on delete restrict on update restrict;
create index ix_measurement_units_dimension_54 on measurement_units (dimension_id);
alter table part_history add constraint fk_part_history_parentPart_55 foreign key (parent_part_id) references parts (id) on delete restrict on update restrict;
create index ix_part_history_parentPart_55 on part_history (parent_part_id);
alter table part_history add constraint fk_part_history_parentMaintenance_56 foreign key (parent_maintenance_id) references maintenances (id) on delete restrict on update restrict;
create index ix_part_history_parentMaintenance_56 on part_history (parent_maintenance_id);
alter table part_history add constraint fk_part_history_eventType_57 foreign key (event_type_id) references inventory_events (id) on delete restrict on update restrict;
create index ix_part_history_eventType_57 on part_history (event_type_id);
alter table part_history add constraint fk_part_history_requester_58 foreign key (requester_id) references users (id) on delete restrict on update restrict;
create index ix_part_history_requester_58 on part_history (requester_id);
alter table part_history add constraint fk_part_history_registrar_59 foreign key (registrar_id) references users (id) on delete restrict on update restrict;
create index ix_part_history_registrar_59 on part_history (registrar_id);
alter table part_history add constraint fk_part_history_attachedDoc_60 foreign key (attached_doc_id) references blobs (id) on delete restrict on update restrict;
create index ix_part_history_attachedDoc_60 on part_history (attached_doc_id);
alter table parts add constraint fk_parts_originalVendor_61 foreign key (original_vendor_id) references companies (id) on delete restrict on update restrict;
create index ix_parts_originalVendor_61 on parts (original_vendor_id);
alter table parts add constraint fk_parts_measurementUnit_62 foreign key (measurement_unit_id) references measurement_units (id) on delete restrict on update restrict;
create index ix_parts_measurementUnit_62 on parts (measurement_unit_id);
alter table parts_components add constraint fk_parts_components_component_63 foreign key (component_id) references components (id) on delete restrict on update restrict;
create index ix_parts_components_component_63 on parts_components (component_id);
alter table parts_components add constraint fk_parts_components_part_64 foreign key (part_id) references parts (id) on delete restrict on update restrict;
create index ix_parts_components_part_64 on parts_components (part_id);
alter table plants add constraint fk_plants_installation_65 foreign key (installation_id) references installations (id) on delete restrict on update restrict;
create index ix_plants_installation_65 on plants (installation_id);
alter table preventive_maitenances add constraint fk_preventive_maitenances_parentPlant_66 foreign key (parent_plant_id) references plants (id) on delete restrict on update restrict;
create index ix_preventive_maitenances_parentPlant_66 on preventive_maitenances (parent_plant_id);
alter table preventive_maitenances add constraint fk_preventive_maitenances_parentSection_67 foreign key (parent_section_id) references sections (id) on delete restrict on update restrict;
create index ix_preventive_maitenances_parentSection_67 on preventive_maitenances (parent_section_id);
alter table preventive_maitenances add constraint fk_preventive_maitenances_parentEquipment_68 foreign key (parent_equipment_id) references equipments (id) on delete restrict on update restrict;
create index ix_preventive_maitenances_parentEquipment_68 on preventive_maitenances (parent_equipment_id);
alter table preventive_maitenances add constraint fk_preventive_maitenances_actingDiscipline_69 foreign key (acting_discipline_id) references disciplines (id) on delete restrict on update restrict;
create index ix_preventive_maitenances_actingDiscipline_69 on preventive_maitenances (acting_discipline_id);
alter table preventive_maitenances add constraint fk_preventive_maitenances_pmClass_70 foreign key (pm_class_id) references equipment_class (id) on delete restrict on update restrict;
create index ix_preventive_maitenances_pmClass_70 on preventive_maitenances (pm_class_id);
alter table repairtools add constraint fk_repairtools_manufacturerCompany_71 foreign key (manufacturer_company_id) references companies (id) on delete restrict on update restrict;
create index ix_repairtools_manufacturerCompany_71 on repairtools (manufacturer_company_id);
alter table repairtools add constraint fk_repairtools_owner_72 foreign key (owner_id) references disciplines (id) on delete restrict on update restrict;
create index ix_repairtools_owner_72 on repairtools (owner_id);
alter table sections add constraint fk_sections_plant_73 foreign key (plant_id) references plants (id) on delete restrict on update restrict;
create index ix_sections_plant_73 on sections (plant_id);
alter table shifts add constraint fk_shifts_user_74 foreign key (user_id) references users (id) on delete restrict on update restrict;
create index ix_shifts_user_74 on shifts (user_id);
alter table shifts add constraint fk_shifts_workType_75 foreign key (work_type_id) references work_types (id) on delete restrict on update restrict;
create index ix_shifts_workType_75 on shifts (work_type_id);
alter table subunits add constraint fk_subunits_equipment_76 foreign key (equipment_id) references equipments (id) on delete restrict on update restrict;
create index ix_subunits_equipment_76 on subunits (equipment_id);
alter table user_blobs add constraint fk_user_blobs_owner_77 foreign key (owner_id) references users (id) on delete restrict on update restrict;
create index ix_user_blobs_owner_77 on user_blobs (owner_id);
alter table users add constraint fk_users_discipline_78 foreign key (discipline_id) references disciplines (id) on delete restrict on update restrict;
create index ix_users_discipline_78 on users (discipline_id);
alter table users add constraint fk_users_hiringCompany_79 foreign key (hiring_company_id) references companies (id) on delete restrict on update restrict;
create index ix_users_hiringCompany_79 on users (hiring_company_id);
alter table users add constraint fk_users_hiringType_80 foreign key (hiring_type_id) references hiring_types (id) on delete restrict on update restrict;
create index ix_users_hiringType_80 on users (hiring_type_id);
alter table workflow_tree add constraint fk_workflow_tree_decidingRole_81 foreign key (deciding_role_id) references security_role (id) on delete restrict on update restrict;
create index ix_workflow_tree_decidingRole_81 on workflow_tree (deciding_role_id);
alter table workflow_tree add constraint fk_workflow_tree_referringRole_82 foreign key (referring_role_id) references security_role (id) on delete restrict on update restrict;
create index ix_workflow_tree_referringRole_82 on workflow_tree (referring_role_id);
alter table workflow_tree add constraint fk_workflow_tree_receivingRole_83 foreign key (receiving_role_id) references security_role (id) on delete restrict on update restrict;
create index ix_workflow_tree_receivingRole_83 on workflow_tree (receiving_role_id);



alter table companies_companies add constraint fk_companies_companies_companies_01 foreign key (head_companies_id) references companies (id) on delete restrict on update restrict;

alter table companies_companies add constraint fk_companies_companies_companies_02 foreign key (rep_companies_id) references companies (id) on delete restrict on update restrict;

alter table components_blobs add constraint fk_components_blobs_components_01 foreign key (components_id) references components (id) on delete restrict on update restrict;

alter table components_blobs add constraint fk_components_blobs_blobs_02 foreign key (blobs_id) references blobs (id) on delete restrict on update restrict;

alter table equipment_class_failure_modes add constraint fk_equipment_class_failure_modes_equipment_class_01 foreign key (equipment_class_id) references equipment_class (id) on delete restrict on update restrict;

alter table equipment_class_failure_modes add constraint fk_equipment_class_failure_modes_failure_modes_02 foreign key (failure_modes_id) references failure_modes (id) on delete restrict on update restrict;

alter table equipments_repairtools add constraint fk_equipments_repairtools_equipments_01 foreign key (equipments_id) references equipments (id) on delete restrict on update restrict;

alter table equipments_repairtools add constraint fk_equipments_repairtools_repairtools_02 foreign key (repairtools_id) references repairtools (id) on delete restrict on update restrict;

alter table equipments_blobs add constraint fk_equipments_blobs_equipments_01 foreign key (equipments_id) references equipments (id) on delete restrict on update restrict;

alter table equipments_blobs add constraint fk_equipments_blobs_blobs_02 foreign key (blobs_id) references blobs (id) on delete restrict on update restrict;

alter table failures_failure_causes add constraint fk_failures_failure_causes_failures_01 foreign key (failures_id) references failures (id) on delete restrict on update restrict;

alter table failures_failure_causes add constraint fk_failures_failure_causes_failure_causes_02 foreign key (failure_causes_id) references failure_causes (id) on delete restrict on update restrict;

alter table installations_blobs add constraint fk_installations_blobs_installations_01 foreign key (installations_id) references installations (id) on delete restrict on update restrict;

alter table installations_blobs add constraint fk_installations_blobs_blobs_02 foreign key (blobs_id) references blobs (id) on delete restrict on update restrict;

alter table maintenances_maintenances add constraint fk_maintenances_maintenances_maintenances_01 foreign key (prereq_maintenances_id) references maintenances (id) on delete restrict on update restrict;

alter table maintenances_maintenances add constraint fk_maintenances_maintenances_maintenances_02 foreign key (dependent_maintenances_id) references maintenances (id) on delete restrict on update restrict;

alter table maintenances_failures add constraint fk_maintenances_failures_maintenances_01 foreign key (maintenances_id) references maintenances (id) on delete restrict on update restrict;

alter table maintenances_failures add constraint fk_maintenances_failures_failures_02 foreign key (failures_id) references failures (id) on delete restrict on update restrict;

alter table maintenances_equipments add constraint fk_maintenances_equipments_maintenances_01 foreign key (maintenances_id) references maintenances (id) on delete restrict on update restrict;

alter table maintenances_equipments add constraint fk_maintenances_equipments_equipments_02 foreign key (equipments_id) references equipments (id) on delete restrict on update restrict;

alter table maintenances_repairtools add constraint fk_maintenances_repairtools_maintenances_01 foreign key (maintenances_id) references maintenances (id) on delete restrict on update restrict;

alter table maintenances_repairtools add constraint fk_maintenances_repairtools_repairtools_02 foreign key (repairtools_id) references repairtools (id) on delete restrict on update restrict;

alter table maintenances_blobs add constraint fk_maintenances_blobs_maintenances_01 foreign key (maintenances_id) references maintenances (id) on delete restrict on update restrict;

alter table maintenances_blobs add constraint fk_maintenances_blobs_blobs_02 foreign key (blobs_id) references blobs (id) on delete restrict on update restrict;

alter table parts_companies add constraint fk_parts_companies_parts_01 foreign key (parts_id) references parts (id) on delete restrict on update restrict;

alter table parts_companies add constraint fk_parts_companies_companies_02 foreign key (companies_id) references companies (id) on delete restrict on update restrict;

alter table parts_blobs add constraint fk_parts_blobs_parts_01 foreign key (parts_id) references parts (id) on delete restrict on update restrict;

alter table parts_blobs add constraint fk_parts_blobs_blobs_02 foreign key (blobs_id) references blobs (id) on delete restrict on update restrict;

alter table plants_blobs add constraint fk_plants_blobs_plants_01 foreign key (plants_id) references plants (id) on delete restrict on update restrict;

alter table plants_blobs add constraint fk_plants_blobs_blobs_02 foreign key (blobs_id) references blobs (id) on delete restrict on update restrict;

alter table repairtools_blobs add constraint fk_repairtools_blobs_repairtools_01 foreign key (repairtools_id) references repairtools (id) on delete restrict on update restrict;

alter table repairtools_blobs add constraint fk_repairtools_blobs_blobs_02 foreign key (blobs_id) references blobs (id) on delete restrict on update restrict;

alter table sections_blobs add constraint fk_sections_blobs_sections_01 foreign key (sections_id) references sections (id) on delete restrict on update restrict;

alter table sections_blobs add constraint fk_sections_blobs_blobs_02 foreign key (blobs_id) references blobs (id) on delete restrict on update restrict;

alter table subunits_blobs add constraint fk_subunits_blobs_subunits_01 foreign key (subunits_id) references subunits (id) on delete restrict on update restrict;

alter table subunits_blobs add constraint fk_subunits_blobs_blobs_02 foreign key (blobs_id) references blobs (id) on delete restrict on update restrict;

alter table users_user_blobs add constraint fk_users_user_blobs_users_01 foreign key (users_id) references users (id) on delete restrict on update restrict;

alter table users_user_blobs add constraint fk_users_user_blobs_user_blobs_02 foreign key (user_blobs_id) references user_blobs (id) on delete restrict on update restrict;

alter table users_security_role add constraint fk_users_security_role_users_01 foreign key (users_id) references users (id) on delete restrict on update restrict;

alter table users_security_role add constraint fk_users_security_role_security_role_02 foreign key (security_role_id) references security_role (id) on delete restrict on update restrict;

alter table users_user_permission add constraint fk_users_user_permission_users_01 foreign key (users_id) references users (id) on delete restrict on update restrict;

alter table users_user_permission add constraint fk_users_user_permission_user_permission_02 foreign key (user_permission_id) references user_permission (id) on delete restrict on update restrict;

# --- !Downs

SET FOREIGN_KEY_CHECKS=0;

drop table blobs;

drop table companies;

drop table companies_companies;

drop table components;

drop table components_blobs;

drop table datasheets;

drop table dimensions;

drop table disciplines;

drop table equipment_category;

drop table equipment_class;

drop table equipment_class_failure_modes;

drop table equipments;

drop table equipments_repairtools;

drop table equipments_blobs;

drop table failure_causes;

drop table failure_mechanisms;

drop table failure_modes;

drop table failures;

drop table failures_failure_causes;

drop table maintenances_failures;

drop table hiring_types;

drop table history;

drop table holidays;

drop table installations;

drop table installations_blobs;

drop table inventory_events;

drop table maintenance_groups;

drop table workflow;

drop table maintenances;

drop table maintenances_maintenances;

drop table maintenances_equipments;

drop table maintenances_repairtools;

drop table maintenances_blobs;

drop table maintenances_components;

drop table maintenances_parts;

drop table maintenances_subunits;

drop table maintenances_users;

drop table measurement_units;

drop table part_history;

drop table parts;

drop table parts_companies;

drop table parts_blobs;

drop table parts_components;

drop table plants;

drop table plants_blobs;

drop table preventive_maitenances;

drop table repairtools;

drop table repairtools_blobs;

drop table sections;

drop table sections_blobs;

drop table security_role;

drop table shifts;

drop table subunits;

drop table subunits_blobs;

drop table user_blobs;

drop table user_permission;

drop table users;

drop table users_user_blobs;

drop table users_security_role;

drop table users_user_permission;

drop table work_types;

drop table workflow_tree;

SET FOREIGN_KEY_CHECKS=1;

