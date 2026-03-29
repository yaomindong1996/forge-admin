DROP TABLE IF EXISTS `act_app_appdef`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_app_appdef`
(
    `ID_`            varchar(255) NOT NULL,
    `REV_`           int          NOT NULL,
    `NAME_`          varchar(255)  DEFAULT NULL,
    `KEY_`           varchar(255) NOT NULL,
    `VERSION_`       int          NOT NULL,
    `CATEGORY_`      varchar(255)  DEFAULT NULL,
    `DEPLOYMENT_ID_` varchar(255)  DEFAULT NULL,
    `RESOURCE_NAME_` varchar(4000) DEFAULT NULL,
    `DESCRIPTION_`   varchar(4000) DEFAULT NULL,
    `TENANT_ID_`     varchar(255)  DEFAULT '',
    PRIMARY KEY (`ID_`),
    UNIQUE KEY `ACT_IDX_APP_DEF_UNIQ` (`KEY_`,`VERSION_`,`TENANT_ID_`),
    KEY              `ACT_IDX_APP_DEF_DPLY` (`DEPLOYMENT_ID_`),
    CONSTRAINT `ACT_FK_APP_DEF_DPLY` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `act_app_deployment` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_app_databasechangelog`
--

DROP TABLE IF EXISTS `act_app_databasechangelog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_app_databasechangelog`
(
    `ID`            varchar(255) NOT NULL,
    `AUTHOR`        varchar(255) NOT NULL,
    `FILENAME`      varchar(255) NOT NULL,
    `DATEEXECUTED`  datetime     NOT NULL,
    `ORDEREXECUTED` int          NOT NULL,
    `EXECTYPE`      varchar(10)  NOT NULL,
    `MD5SUM`        varchar(35)  DEFAULT NULL,
    `DESCRIPTION`   varchar(255) DEFAULT NULL,
    `COMMENTS`      varchar(255) DEFAULT NULL,
    `TAG`           varchar(255) DEFAULT NULL,
    `LIQUIBASE`     varchar(20)  DEFAULT NULL,
    `CONTEXTS`      varchar(255) DEFAULT NULL,
    `LABELS`        varchar(255) DEFAULT NULL,
    `DEPLOYMENT_ID` varchar(10)  DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_app_databasechangeloglock`
--

DROP TABLE IF EXISTS `act_app_databasechangeloglock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_app_databasechangeloglock`
(
    `ID`          int NOT NULL,
    `LOCKED`      tinyint(1) NOT NULL,
    `LOCKGRANTED` datetime     DEFAULT NULL,
    `LOCKEDBY`    varchar(255) DEFAULT NULL,
    PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_app_deployment`
--

DROP TABLE IF EXISTS `act_app_deployment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_app_deployment`
(
    `ID_`          varchar(255) NOT NULL,
    `NAME_`        varchar(255) DEFAULT NULL,
    `CATEGORY_`    varchar(255) DEFAULT NULL,
    `KEY_`         varchar(255) DEFAULT NULL,
    `DEPLOY_TIME_` datetime(3) DEFAULT NULL,
    `TENANT_ID_`   varchar(255) DEFAULT '',
    PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_app_deployment_resource`
--

DROP TABLE IF EXISTS `act_app_deployment_resource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_app_deployment_resource`
(
    `ID_`             varchar(255) NOT NULL,
    `NAME_`           varchar(255) DEFAULT NULL,
    `DEPLOYMENT_ID_`  varchar(255) DEFAULT NULL,
    `RESOURCE_BYTES_` longblob,
    PRIMARY KEY (`ID_`),
    KEY               `ACT_IDX_APP_RSRC_DPL` (`DEPLOYMENT_ID_`),
    CONSTRAINT `ACT_FK_APP_RSRC_DPL` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `act_app_deployment` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_cmmn_casedef`
--

DROP TABLE IF EXISTS `act_cmmn_casedef`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_casedef`
(
    `ID_`                     varchar(255) NOT NULL,
    `REV_`                    int          NOT NULL,
    `NAME_`                   varchar(255)  DEFAULT NULL,
    `KEY_`                    varchar(255) NOT NULL,
    `VERSION_`                int          NOT NULL,
    `CATEGORY_`               varchar(255)  DEFAULT NULL,
    `DEPLOYMENT_ID_`          varchar(255)  DEFAULT NULL,
    `RESOURCE_NAME_`          varchar(4000) DEFAULT NULL,
    `DESCRIPTION_`            varchar(4000) DEFAULT NULL,
    `HAS_GRAPHICAL_NOTATION_` tinyint(1) DEFAULT NULL,
    `TENANT_ID_`              varchar(255)  DEFAULT '',
    `DGRM_RESOURCE_NAME_`     varchar(4000) DEFAULT NULL,
    `HAS_START_FORM_KEY_`     tinyint(1) DEFAULT NULL,
    PRIMARY KEY (`ID_`),
    UNIQUE KEY `ACT_IDX_CASE_DEF_UNIQ` (`KEY_`,`VERSION_`,`TENANT_ID_`),
    KEY                       `ACT_IDX_CASE_DEF_DPLY` (`DEPLOYMENT_ID_`),
    CONSTRAINT `ACT_FK_CASE_DEF_DPLY` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `act_cmmn_deployment` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_cmmn_databasechangelog`
--

DROP TABLE IF EXISTS `act_cmmn_databasechangelog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_databasechangelog`
(
    `ID`            varchar(255) NOT NULL,
    `AUTHOR`        varchar(255) NOT NULL,
    `FILENAME`      varchar(255) NOT NULL,
    `DATEEXECUTED`  datetime     NOT NULL,
    `ORDEREXECUTED` int          NOT NULL,
    `EXECTYPE`      varchar(10)  NOT NULL,
    `MD5SUM`        varchar(35)  DEFAULT NULL,
    `DESCRIPTION`   varchar(255) DEFAULT NULL,
    `COMMENTS`      varchar(255) DEFAULT NULL,
    `TAG`           varchar(255) DEFAULT NULL,
    `LIQUIBASE`     varchar(20)  DEFAULT NULL,
    `CONTEXTS`      varchar(255) DEFAULT NULL,
    `LABELS`        varchar(255) DEFAULT NULL,
    `DEPLOYMENT_ID` varchar(10)  DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_cmmn_databasechangeloglock`
--

DROP TABLE IF EXISTS `act_cmmn_databasechangeloglock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_databasechangeloglock`
(
    `ID`          int NOT NULL,
    `LOCKED`      tinyint(1) NOT NULL,
    `LOCKGRANTED` datetime     DEFAULT NULL,
    `LOCKEDBY`    varchar(255) DEFAULT NULL,
    PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_cmmn_deployment`
--

DROP TABLE IF EXISTS `act_cmmn_deployment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_deployment`
(
    `ID_`                   varchar(255) NOT NULL,
    `NAME_`                 varchar(255) DEFAULT NULL,
    `CATEGORY_`             varchar(255) DEFAULT NULL,
    `KEY_`                  varchar(255) DEFAULT NULL,
    `DEPLOY_TIME_`          datetime(3) DEFAULT NULL,
    `PARENT_DEPLOYMENT_ID_` varchar(255) DEFAULT NULL,
    `TENANT_ID_`            varchar(255) DEFAULT '',
    PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_cmmn_deployment_resource`
--

DROP TABLE IF EXISTS `act_cmmn_deployment_resource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_deployment_resource`
(
    `ID_`             varchar(255) NOT NULL,
    `NAME_`           varchar(255) DEFAULT NULL,
    `DEPLOYMENT_ID_`  varchar(255) DEFAULT NULL,
    `RESOURCE_BYTES_` longblob,
    `GENERATED_`      tinyint(1) DEFAULT NULL,
    PRIMARY KEY (`ID_`),
    KEY               `ACT_IDX_CMMN_RSRC_DPL` (`DEPLOYMENT_ID_`),
    CONSTRAINT `ACT_FK_CMMN_RSRC_DPL` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `act_cmmn_deployment` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_cmmn_hi_case_inst`
--

DROP TABLE IF EXISTS `act_cmmn_hi_case_inst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_hi_case_inst`
(
    `ID_`                        varchar(255) NOT NULL,
    `REV_`                       int          NOT NULL,
    `BUSINESS_KEY_`              varchar(255) DEFAULT NULL,
    `NAME_`                      varchar(255) DEFAULT NULL,
    `PARENT_ID_`                 varchar(255) DEFAULT NULL,
    `CASE_DEF_ID_`               varchar(255) DEFAULT NULL,
    `STATE_`                     varchar(255) DEFAULT NULL,
    `START_TIME_`                datetime(3) DEFAULT NULL,
    `END_TIME_`                  datetime(3) DEFAULT NULL,
    `START_USER_ID_`             varchar(255) DEFAULT NULL,
    `CALLBACK_ID_`               varchar(255) DEFAULT NULL,
    `CALLBACK_TYPE_`             varchar(255) DEFAULT NULL,
    `TENANT_ID_`                 varchar(255) DEFAULT '',
    `REFERENCE_ID_`              varchar(255) DEFAULT NULL,
    `REFERENCE_TYPE_`            varchar(255) DEFAULT NULL,
    `LAST_REACTIVATION_TIME_`    datetime(3) DEFAULT NULL,
    `LAST_REACTIVATION_USER_ID_` varchar(255) DEFAULT NULL,
    `BUSINESS_STATUS_`           varchar(255) DEFAULT NULL,
    PRIMARY KEY (`ID_`),
    KEY                          `ACT_IDX_HI_CASE_INST_END` (`END_TIME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_cmmn_hi_mil_inst`
--

DROP TABLE IF EXISTS `act_cmmn_hi_mil_inst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_hi_mil_inst`
(
    `ID_`           varchar(255) NOT NULL,
    `REV_`          int          NOT NULL,
    `NAME_`         varchar(255) NOT NULL,
    `TIME_STAMP_`   datetime(3) DEFAULT NULL,
    `CASE_INST_ID_` varchar(255) NOT NULL,
    `CASE_DEF_ID_`  varchar(255) NOT NULL,
    `ELEMENT_ID_`   varchar(255) NOT NULL,
    `TENANT_ID_`    varchar(255) DEFAULT '',
    PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_cmmn_hi_plan_item_inst`
--

DROP TABLE IF EXISTS `act_cmmn_hi_plan_item_inst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_hi_plan_item_inst`
(
    `ID_`                    varchar(255) NOT NULL,
    `REV_`                   int          NOT NULL,
    `NAME_`                  varchar(255) DEFAULT NULL,
    `STATE_`                 varchar(255) DEFAULT NULL,
    `CASE_DEF_ID_`           varchar(255) DEFAULT NULL,
    `CASE_INST_ID_`          varchar(255) DEFAULT NULL,
    `STAGE_INST_ID_`         varchar(255) DEFAULT NULL,
    `IS_STAGE_`              tinyint(1) DEFAULT NULL,
    `ELEMENT_ID_`            varchar(255) DEFAULT NULL,
    `ITEM_DEFINITION_ID_`    varchar(255) DEFAULT NULL,
    `ITEM_DEFINITION_TYPE_`  varchar(255) DEFAULT NULL,
    `CREATE_TIME_`           datetime(3) DEFAULT NULL,
    `LAST_AVAILABLE_TIME_`   datetime(3) DEFAULT NULL,
    `LAST_ENABLED_TIME_`     datetime(3) DEFAULT NULL,
    `LAST_DISABLED_TIME_`    datetime(3) DEFAULT NULL,
    `LAST_STARTED_TIME_`     datetime(3) DEFAULT NULL,
    `LAST_SUSPENDED_TIME_`   datetime(3) DEFAULT NULL,
    `COMPLETED_TIME_`        datetime(3) DEFAULT NULL,
    `OCCURRED_TIME_`         datetime(3) DEFAULT NULL,
    `TERMINATED_TIME_`       datetime(3) DEFAULT NULL,
    `EXIT_TIME_`             datetime(3) DEFAULT NULL,
    `ENDED_TIME_`            datetime(3) DEFAULT NULL,
    `LAST_UPDATED_TIME_`     datetime(3) DEFAULT NULL,
    `START_USER_ID_`         varchar(255) DEFAULT NULL,
    `REFERENCE_ID_`          varchar(255) DEFAULT NULL,
    `REFERENCE_TYPE_`        varchar(255) DEFAULT NULL,
    `TENANT_ID_`             varchar(255) DEFAULT '',
    `ENTRY_CRITERION_ID_`    varchar(255) DEFAULT NULL,
    `EXIT_CRITERION_ID_`     varchar(255) DEFAULT NULL,
    `SHOW_IN_OVERVIEW_`      tinyint(1) DEFAULT NULL,
    `EXTRA_VALUE_`           varchar(255) DEFAULT NULL,
    `DERIVED_CASE_DEF_ID_`   varchar(255) DEFAULT NULL,
    `LAST_UNAVAILABLE_TIME_` datetime(3) DEFAULT NULL,
    PRIMARY KEY (`ID_`),
    KEY                      `ACT_IDX_HI_PLAN_ITEM_INST_CASE` (`CASE_INST_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_cmmn_ru_case_inst`
--

DROP TABLE IF EXISTS `act_cmmn_ru_case_inst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_ru_case_inst`
(
    `ID_`                        varchar(255) NOT NULL,
    `REV_`                       int          NOT NULL,
    `BUSINESS_KEY_`              varchar(255) DEFAULT NULL,
    `NAME_`                      varchar(255) DEFAULT NULL,
    `PARENT_ID_`                 varchar(255) DEFAULT NULL,
    `CASE_DEF_ID_`               varchar(255) DEFAULT NULL,
    `STATE_`                     varchar(255) DEFAULT NULL,
    `START_TIME_`                datetime(3) DEFAULT NULL,
    `START_USER_ID_`             varchar(255) DEFAULT NULL,
    `CALLBACK_ID_`               varchar(255) DEFAULT NULL,
    `CALLBACK_TYPE_`             varchar(255) DEFAULT NULL,
    `TENANT_ID_`                 varchar(255) DEFAULT '',
    `LOCK_TIME_`                 datetime(3) DEFAULT NULL,
    `IS_COMPLETEABLE_`           tinyint(1) DEFAULT NULL,
    `REFERENCE_ID_`              varchar(255) DEFAULT NULL,
    `REFERENCE_TYPE_`            varchar(255) DEFAULT NULL,
    `LOCK_OWNER_`                varchar(255) DEFAULT NULL,
    `LAST_REACTIVATION_TIME_`    datetime(3) DEFAULT NULL,
    `LAST_REACTIVATION_USER_ID_` varchar(255) DEFAULT NULL,
    `BUSINESS_STATUS_`           varchar(255) DEFAULT NULL,
    PRIMARY KEY (`ID_`),
    KEY                          `ACT_IDX_CASE_INST_CASE_DEF` (`CASE_DEF_ID_`),
    KEY                          `ACT_IDX_CASE_INST_PARENT` (`PARENT_ID_`),
    KEY                          `ACT_IDX_CASE_INST_REF_ID_` (`REFERENCE_ID_`),
    CONSTRAINT `ACT_FK_CASE_INST_CASE_DEF` FOREIGN KEY (`CASE_DEF_ID_`) REFERENCES `act_cmmn_casedef` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_cmmn_ru_mil_inst`
--

DROP TABLE IF EXISTS `act_cmmn_ru_mil_inst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_ru_mil_inst`
(
    `ID_`           varchar(255) NOT NULL,
    `NAME_`         varchar(255) NOT NULL,
    `TIME_STAMP_`   datetime(3) DEFAULT NULL,
    `CASE_INST_ID_` varchar(255) NOT NULL,
    `CASE_DEF_ID_`  varchar(255) NOT NULL,
    `ELEMENT_ID_`   varchar(255) NOT NULL,
    `TENANT_ID_`    varchar(255) DEFAULT '',
    PRIMARY KEY (`ID_`),
    KEY             `ACT_IDX_MIL_CASE_DEF` (`CASE_DEF_ID_`),
    KEY             `ACT_IDX_MIL_CASE_INST` (`CASE_INST_ID_`),
    CONSTRAINT `ACT_FK_MIL_CASE_DEF` FOREIGN KEY (`CASE_DEF_ID_`) REFERENCES `act_cmmn_casedef` (`ID_`),
    CONSTRAINT `ACT_FK_MIL_CASE_INST` FOREIGN KEY (`CASE_INST_ID_`) REFERENCES `act_cmmn_ru_case_inst` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_cmmn_ru_plan_item_inst`
--

DROP TABLE IF EXISTS `act_cmmn_ru_plan_item_inst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_ru_plan_item_inst`
(
    `ID_`                     varchar(255) NOT NULL,
    `REV_`                    int          NOT NULL,
    `CASE_DEF_ID_`            varchar(255) DEFAULT NULL,
    `CASE_INST_ID_`           varchar(255) DEFAULT NULL,
    `STAGE_INST_ID_`          varchar(255) DEFAULT NULL,
    `IS_STAGE_`               tinyint(1) DEFAULT NULL,
    `ELEMENT_ID_`             varchar(255) DEFAULT NULL,
    `NAME_`                   varchar(255) DEFAULT NULL,
    `STATE_`                  varchar(255) DEFAULT NULL,
    `CREATE_TIME_`            datetime(3) DEFAULT NULL,
    `START_USER_ID_`          varchar(255) DEFAULT NULL,
    `REFERENCE_ID_`           varchar(255) DEFAULT NULL,
    `REFERENCE_TYPE_`         varchar(255) DEFAULT NULL,
    `TENANT_ID_`              varchar(255) DEFAULT '',
    `ITEM_DEFINITION_ID_`     varchar(255) DEFAULT NULL,
    `ITEM_DEFINITION_TYPE_`   varchar(255) DEFAULT NULL,
    `IS_COMPLETEABLE_`        tinyint(1) DEFAULT NULL,
    `IS_COUNT_ENABLED_`       tinyint(1) DEFAULT NULL,
    `VAR_COUNT_`              int          DEFAULT NULL,
    `SENTRY_PART_INST_COUNT_` int          DEFAULT NULL,
    `LAST_AVAILABLE_TIME_`    datetime(3) DEFAULT NULL,
    `LAST_ENABLED_TIME_`      datetime(3) DEFAULT NULL,
    `LAST_DISABLED_TIME_`     datetime(3) DEFAULT NULL,
    `LAST_STARTED_TIME_`      datetime(3) DEFAULT NULL,
    `LAST_SUSPENDED_TIME_`    datetime(3) DEFAULT NULL,
    `COMPLETED_TIME_`         datetime(3) DEFAULT NULL,
    `OCCURRED_TIME_`          datetime(3) DEFAULT NULL,
    `TERMINATED_TIME_`        datetime(3) DEFAULT NULL,
    `EXIT_TIME_`              datetime(3) DEFAULT NULL,
    `ENDED_TIME_`             datetime(3) DEFAULT NULL,
    `ENTRY_CRITERION_ID_`     varchar(255) DEFAULT NULL,
    `EXIT_CRITERION_ID_`      varchar(255) DEFAULT NULL,
    `EXTRA_VALUE_`            varchar(255) DEFAULT NULL,
    `DERIVED_CASE_DEF_ID_`    varchar(255) DEFAULT NULL,
    `LAST_UNAVAILABLE_TIME_`  datetime(3) DEFAULT NULL,
    PRIMARY KEY (`ID_`),
    KEY                       `ACT_IDX_PLAN_ITEM_CASE_DEF` (`CASE_DEF_ID_`),
    KEY                       `ACT_IDX_PLAN_ITEM_CASE_INST` (`CASE_INST_ID_`),
    KEY                       `ACT_IDX_PLAN_ITEM_STAGE_INST` (`STAGE_INST_ID_`),
    CONSTRAINT `ACT_FK_PLAN_ITEM_CASE_DEF` FOREIGN KEY (`CASE_DEF_ID_`) REFERENCES `act_cmmn_casedef` (`ID_`),
    CONSTRAINT `ACT_FK_PLAN_ITEM_CASE_INST` FOREIGN KEY (`CASE_INST_ID_`) REFERENCES `act_cmmn_ru_case_inst` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_cmmn_ru_sentry_part_inst`
--

DROP TABLE IF EXISTS `act_cmmn_ru_sentry_part_inst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_ru_sentry_part_inst`
(
    `ID_`                varchar(255) NOT NULL,
    `REV_`               int          NOT NULL,
    `CASE_DEF_ID_`       varchar(255) DEFAULT NULL,
    `CASE_INST_ID_`      varchar(255) DEFAULT NULL,
    `PLAN_ITEM_INST_ID_` varchar(255) DEFAULT NULL,
    `ON_PART_ID_`        varchar(255) DEFAULT NULL,
    `IF_PART_ID_`        varchar(255) DEFAULT NULL,
    `TIME_STAMP_`        datetime(3) DEFAULT NULL,
    PRIMARY KEY (`ID_`),
    KEY                  `ACT_IDX_SENTRY_CASE_DEF` (`CASE_DEF_ID_`),
    KEY                  `ACT_IDX_SENTRY_CASE_INST` (`CASE_INST_ID_`),
    KEY                  `ACT_IDX_SENTRY_PLAN_ITEM` (`PLAN_ITEM_INST_ID_`),
    CONSTRAINT `ACT_FK_SENTRY_CASE_DEF` FOREIGN KEY (`CASE_DEF_ID_`) REFERENCES `act_cmmn_casedef` (`ID_`),
    CONSTRAINT `ACT_FK_SENTRY_CASE_INST` FOREIGN KEY (`CASE_INST_ID_`) REFERENCES `act_cmmn_ru_case_inst` (`ID_`),
    CONSTRAINT `ACT_FK_SENTRY_PLAN_ITEM` FOREIGN KEY (`PLAN_ITEM_INST_ID_`) REFERENCES `act_cmmn_ru_plan_item_inst` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_dmn_databasechangelog`
--

DROP TABLE IF EXISTS `act_dmn_databasechangelog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_dmn_databasechangelog`
(
    `ID`            varchar(255) NOT NULL,
    `AUTHOR`        varchar(255) NOT NULL,
    `FILENAME`      varchar(255) NOT NULL,
    `DATEEXECUTED`  datetime     NOT NULL,
    `ORDEREXECUTED` int          NOT NULL,
    `EXECTYPE`      varchar(10)  NOT NULL,
    `MD5SUM`        varchar(35)  DEFAULT NULL,
    `DESCRIPTION`   varchar(255) DEFAULT NULL,
    `COMMENTS`      varchar(255) DEFAULT NULL,
    `TAG`           varchar(255) DEFAULT NULL,
    `LIQUIBASE`     varchar(20)  DEFAULT NULL,
    `CONTEXTS`      varchar(255) DEFAULT NULL,
    `LABELS`        varchar(255) DEFAULT NULL,
    `DEPLOYMENT_ID` varchar(10)  DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_dmn_databasechangeloglock`
--

DROP TABLE IF EXISTS `act_dmn_databasechangeloglock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_dmn_databasechangeloglock`
(
    `ID`          int NOT NULL,
    `LOCKED`      tinyint(1) NOT NULL,
    `LOCKGRANTED` datetime     DEFAULT NULL,
    `LOCKEDBY`    varchar(255) DEFAULT NULL,
    PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_dmn_decision`
--

DROP TABLE IF EXISTS `act_dmn_decision`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_dmn_decision`
(
    `ID_`            varchar(255) NOT NULL,
    `NAME_`          varchar(255) DEFAULT NULL,
    `VERSION_`       int          DEFAULT NULL,
    `KEY_`           varchar(255) DEFAULT NULL,
    `CATEGORY_`      varchar(255) DEFAULT NULL,
    `DEPLOYMENT_ID_` varchar(255) DEFAULT NULL,
    `TENANT_ID_`     varchar(255) DEFAULT NULL,
    `RESOURCE_NAME_` varchar(255) DEFAULT NULL,
    `DESCRIPTION_`   varchar(255) DEFAULT NULL,
    `DECISION_TYPE_` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`ID_`),
    UNIQUE KEY `ACT_IDX_DMN_DEC_UNIQ` (`KEY_`,`VERSION_`,`TENANT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_dmn_deployment`
--

DROP TABLE IF EXISTS `act_dmn_deployment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_dmn_deployment`
(
    `ID_`                   varchar(255) NOT NULL,
    `NAME_`                 varchar(255) DEFAULT NULL,
    `CATEGORY_`             varchar(255) DEFAULT NULL,
    `DEPLOY_TIME_`          datetime(3) DEFAULT NULL,
    `TENANT_ID_`            varchar(255) DEFAULT NULL,
    `PARENT_DEPLOYMENT_ID_` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_dmn_deployment_resource`
--

DROP TABLE IF EXISTS `act_dmn_deployment_resource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_dmn_deployment_resource`
(
    `ID_`             varchar(255) NOT NULL,
    `NAME_`           varchar(255) DEFAULT NULL,
    `DEPLOYMENT_ID_`  varchar(255) DEFAULT NULL,
    `RESOURCE_BYTES_` longblob,
    PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_dmn_hi_decision_execution`
--

DROP TABLE IF EXISTS `act_dmn_hi_decision_execution`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_dmn_hi_decision_execution`
(
    `ID_`                     varchar(255) NOT NULL,
    `DECISION_DEFINITION_ID_` varchar(255) DEFAULT NULL,
    `DEPLOYMENT_ID_`          varchar(255) DEFAULT NULL,
    `START_TIME_`             datetime(3) DEFAULT NULL,
    `END_TIME_`               datetime(3) DEFAULT NULL,
    `INSTANCE_ID_`            varchar(255) DEFAULT NULL,
    `EXECUTION_ID_`           varchar(255) DEFAULT NULL,
    `ACTIVITY_ID_`            varchar(255) DEFAULT NULL,
    `FAILED_`                 tinyint(1) DEFAULT '0',
    `TENANT_ID_`              varchar(255) DEFAULT NULL,
    `EXECUTION_JSON_`         longtext,
    `SCOPE_TYPE_`             varchar(255) DEFAULT NULL,
    PRIMARY KEY (`ID_`),
    KEY                       `ACT_IDX_DMN_INSTANCE_ID` (`INSTANCE_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_evt_log`
--

DROP TABLE IF EXISTS `act_evt_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_evt_log`
(
    `LOG_NR_`       bigint       NOT NULL AUTO_INCREMENT,
    `TYPE_`         varchar(64) COLLATE utf8mb3_bin  DEFAULT NULL,
    `PROC_DEF_ID_`  varchar(64) COLLATE utf8mb3_bin  DEFAULT NULL,
    `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin  DEFAULT NULL,
    `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin  DEFAULT NULL,
    `TASK_ID_`      varchar(64) COLLATE utf8mb3_bin  DEFAULT NULL,
    `TIME_STAMP_`   timestamp(3) NOT NULL            DEFAULT CURRENT_TIMESTAMP(3),
    `USER_ID_`      varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `DATA_`         longblob,
    `LOCK_OWNER_`   varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `LOCK_TIME_`    timestamp(3) NULL DEFAULT NULL,
    `IS_PROCESSED_` tinyint                          DEFAULT '0',
    PRIMARY KEY (`LOG_NR_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_ge_bytearray`
--

DROP TABLE IF EXISTS `act_ge_bytearray`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ge_bytearray`
(
    `ID_`            varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `REV_`           int                              DEFAULT NULL,
    `NAME_`          varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `DEPLOYMENT_ID_` varchar(64) COLLATE utf8mb3_bin  DEFAULT NULL,
    `BYTES_`         longblob,
    `GENERATED_`     tinyint                          DEFAULT NULL,
    PRIMARY KEY (`ID_`),
    KEY              `ACT_FK_BYTEARR_DEPL` (`DEPLOYMENT_ID_`),
    CONSTRAINT `ACT_FK_BYTEARR_DEPL` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `act_re_deployment` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_ge_property`
--

DROP TABLE IF EXISTS `act_ge_property`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ge_property`
(
    `NAME_`  varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `VALUE_` varchar(300) COLLATE utf8mb3_bin DEFAULT NULL,
    `REV_`   int                              DEFAULT NULL,
    PRIMARY KEY (`NAME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_hi_actinst`
--

DROP TABLE IF EXISTS `act_hi_actinst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_actinst`
(
    `ID_`                varchar(64) COLLATE utf8mb3_bin  NOT NULL,
    `REV_`               int                               DEFAULT '1',
    `PROC_DEF_ID_`       varchar(64) COLLATE utf8mb3_bin  NOT NULL,
    `PROC_INST_ID_`      varchar(64) COLLATE utf8mb3_bin  NOT NULL,
    `EXECUTION_ID_`      varchar(64) COLLATE utf8mb3_bin  NOT NULL,
    `ACT_ID_`            varchar(255) COLLATE utf8mb3_bin NOT NULL,
    `TASK_ID_`           varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `CALL_PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `ACT_NAME_`          varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `ACT_TYPE_`          varchar(255) COLLATE utf8mb3_bin NOT NULL,
    `ASSIGNEE_`          varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `START_TIME_`        datetime(3) NOT NULL,
    `END_TIME_`          datetime(3) DEFAULT NULL,
    `TRANSACTION_ORDER_` int                               DEFAULT NULL,
    `DURATION_`          bigint                            DEFAULT NULL,
    `DELETE_REASON_`     varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
    `TENANT_ID_`         varchar(255) COLLATE utf8mb3_bin  DEFAULT '',
    PRIMARY KEY (`ID_`),
    KEY                  `ACT_IDX_HI_ACT_INST_START` (`START_TIME_`),
    KEY                  `ACT_IDX_HI_ACT_INST_END` (`END_TIME_`),
    KEY                  `ACT_IDX_HI_ACT_INST_PROCINST` (`PROC_INST_ID_`,`ACT_ID_`),
    KEY                  `ACT_IDX_HI_ACT_INST_EXEC` (`EXECUTION_ID_`,`ACT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_hi_attachment`
--

DROP TABLE IF EXISTS `act_hi_attachment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_attachment`
(
    `ID_`           varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `REV_`          int                               DEFAULT NULL,
    `USER_ID_`      varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `NAME_`         varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `DESCRIPTION_`  varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
    `TYPE_`         varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `TASK_ID_`      varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `URL_`          varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
    `CONTENT_ID_`   varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `TIME_`         datetime(3) DEFAULT NULL,
    PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_hi_comment`
--

DROP TABLE IF EXISTS `act_hi_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_comment`
(
    `ID_`           varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `TYPE_`         varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `TIME_`         datetime(3) NOT NULL,
    `USER_ID_`      varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `TASK_ID_`      varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `ACTION_`       varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `MESSAGE_`      varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
    `FULL_MSG_`     longblob,
    PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_hi_detail`
--

DROP TABLE IF EXISTS `act_hi_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_detail`
(
    `ID_`           varchar(64) COLLATE utf8mb3_bin  NOT NULL,
    `TYPE_`         varchar(255) COLLATE utf8mb3_bin NOT NULL,
    `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `TASK_ID_`      varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `ACT_INST_ID_`  varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `NAME_`         varchar(255) COLLATE utf8mb3_bin NOT NULL,
    `VAR_TYPE_`     varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `REV_`          int                               DEFAULT NULL,
    `TIME_`         datetime(3) NOT NULL,
    `BYTEARRAY_ID_` varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `DOUBLE_` double DEFAULT NULL,
    `LONG_`         bigint                            DEFAULT NULL,
    `TEXT_`         varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
    `TEXT2_`        varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
    PRIMARY KEY (`ID_`),
    KEY             `ACT_IDX_HI_DETAIL_PROC_INST` (`PROC_INST_ID_`),
    KEY             `ACT_IDX_HI_DETAIL_ACT_INST` (`ACT_INST_ID_`),
    KEY             `ACT_IDX_HI_DETAIL_TIME` (`TIME_`),
    KEY             `ACT_IDX_HI_DETAIL_NAME` (`NAME_`),
    KEY             `ACT_IDX_HI_DETAIL_TASK_ID` (`TASK_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_hi_entitylink`
--

DROP TABLE IF EXISTS `act_hi_entitylink`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_entitylink`
(
    `ID_`                      varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `LINK_TYPE_`               varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `CREATE_TIME_`             datetime(3) DEFAULT NULL,
    `SCOPE_ID_`                varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `SUB_SCOPE_ID_`            varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `SCOPE_TYPE_`              varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `SCOPE_DEFINITION_ID_`     varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `PARENT_ELEMENT_ID_`       varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `REF_SCOPE_ID_`            varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `REF_SCOPE_TYPE_`          varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `REF_SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `ROOT_SCOPE_ID_`           varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `ROOT_SCOPE_TYPE_`         varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `HIERARCHY_TYPE_`          varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    PRIMARY KEY (`ID_`),
    KEY                        `ACT_IDX_HI_ENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`),
    KEY                        `ACT_IDX_HI_ENT_LNK_REF_SCOPE` (`REF_SCOPE_ID_`,`REF_SCOPE_TYPE_`,`LINK_TYPE_`),
    KEY                        `ACT_IDX_HI_ENT_LNK_ROOT_SCOPE` (`ROOT_SCOPE_ID_`,`ROOT_SCOPE_TYPE_`,`LINK_TYPE_`),
    KEY                        `ACT_IDX_HI_ENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_hi_identitylink`
--

DROP TABLE IF EXISTS `act_hi_identitylink`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_identitylink`
(
    `ID_`                  varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `GROUP_ID_`            varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `TYPE_`                varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `USER_ID_`             varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `TASK_ID_`             varchar(64) COLLATE utf8mb3_bin  DEFAULT NULL,
    `CREATE_TIME_`         datetime(3) DEFAULT NULL,
    `PROC_INST_ID_`        varchar(64) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SCOPE_ID_`            varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `SUB_SCOPE_ID_`        varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `SCOPE_TYPE_`          varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    PRIMARY KEY (`ID_`),
    KEY                    `ACT_IDX_HI_IDENT_LNK_USER` (`USER_ID_`),
    KEY                    `ACT_IDX_HI_IDENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
    KEY                    `ACT_IDX_HI_IDENT_LNK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
    KEY                    `ACT_IDX_HI_IDENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
    KEY                    `ACT_IDX_HI_IDENT_LNK_TASK` (`TASK_ID_`),
    KEY                    `ACT_IDX_HI_IDENT_LNK_PROCINST` (`PROC_INST_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_hi_procinst`
--

DROP TABLE IF EXISTS `act_hi_procinst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_procinst`
(
    `ID_`                        varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `REV_`                       int                               DEFAULT '1',
    `PROC_INST_ID_`              varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `BUSINESS_KEY_`              varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `PROC_DEF_ID_`               varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `START_TIME_`                datetime(3) NOT NULL,
    `END_TIME_`                  datetime(3) DEFAULT NULL,
    `DURATION_`                  bigint                            DEFAULT NULL,
    `START_USER_ID_`             varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `START_ACT_ID_`              varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `END_ACT_ID_`                varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SUPER_PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `DELETE_REASON_`             varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
    `TENANT_ID_`                 varchar(255) COLLATE utf8mb3_bin  DEFAULT '',
    `NAME_`                      varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `CALLBACK_ID_`               varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `CALLBACK_TYPE_`             varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `REFERENCE_ID_`              varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `REFERENCE_TYPE_`            varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `PROPAGATED_STAGE_INST_ID_`  varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `BUSINESS_STATUS_`           varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    PRIMARY KEY (`ID_`),
    UNIQUE KEY `PROC_INST_ID_` (`PROC_INST_ID_`),
    KEY                          `ACT_IDX_HI_PRO_INST_END` (`END_TIME_`),
    KEY                          `ACT_IDX_HI_PRO_I_BUSKEY` (`BUSINESS_KEY_`),
    KEY                          `ACT_IDX_HI_PRO_SUPER_PROCINST` (`SUPER_PROCESS_INSTANCE_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_hi_taskinst`
--

DROP TABLE IF EXISTS `act_hi_taskinst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_taskinst`
(
    `ID_`                       varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `REV_`                      int                               DEFAULT '1',
    `PROC_DEF_ID_`              varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `TASK_DEF_ID_`              varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `TASK_DEF_KEY_`             varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `PROC_INST_ID_`             varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `EXECUTION_ID_`             varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `SCOPE_ID_`                 varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SUB_SCOPE_ID_`             varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SCOPE_TYPE_`               varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SCOPE_DEFINITION_ID_`      varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `STATE_`                    varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `NAME_`                     varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `PARENT_TASK_ID_`           varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `DESCRIPTION_`              varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
    `OWNER_`                    varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `ASSIGNEE_`                 varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `START_TIME_`               datetime(3) NOT NULL,
    `IN_PROGRESS_TIME_`         datetime(3) DEFAULT NULL,
    `IN_PROGRESS_STARTED_BY_`   varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `CLAIM_TIME_`               datetime(3) DEFAULT NULL,
    `CLAIMED_BY_`               varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SUSPENDED_TIME_`           datetime(3) DEFAULT NULL,
    `SUSPENDED_BY_`             varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `END_TIME_`                 datetime(3) DEFAULT NULL,
    `COMPLETED_BY_`             varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `DURATION_`                 bigint                            DEFAULT NULL,
    `DELETE_REASON_`            varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
    `PRIORITY_`                 int                               DEFAULT NULL,
    `IN_PROGRESS_DUE_DATE_`     datetime(3) DEFAULT NULL,
    `DUE_DATE_`                 datetime(3) DEFAULT NULL,
    `FORM_KEY_`                 varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `CATEGORY_`                 varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `TENANT_ID_`                varchar(255) COLLATE utf8mb3_bin  DEFAULT '',
    `LAST_UPDATED_TIME_`        datetime(3) DEFAULT NULL,
    PRIMARY KEY (`ID_`),
    KEY                         `ACT_IDX_HI_TASK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
    KEY                         `ACT_IDX_HI_TASK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
    KEY                         `ACT_IDX_HI_TASK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
    KEY                         `ACT_IDX_HI_TASK_INST_PROCINST` (`PROC_INST_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_hi_tsk_log`
--

DROP TABLE IF EXISTS `act_hi_tsk_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_tsk_log`
(
    `ID_`                  bigint                          NOT NULL AUTO_INCREMENT,
    `TYPE_`                varchar(64) COLLATE utf8mb3_bin          DEFAULT NULL,
    `TASK_ID_`             varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `TIME_STAMP_`          timestamp(3)                    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `USER_ID_`             varchar(255) COLLATE utf8mb3_bin         DEFAULT NULL,
    `DATA_`                varchar(4000) COLLATE utf8mb3_bin        DEFAULT NULL,
    `EXECUTION_ID_`        varchar(64) COLLATE utf8mb3_bin          DEFAULT NULL,
    `PROC_INST_ID_`        varchar(64) COLLATE utf8mb3_bin          DEFAULT NULL,
    `PROC_DEF_ID_`         varchar(64) COLLATE utf8mb3_bin          DEFAULT NULL,
    `SCOPE_ID_`            varchar(255) COLLATE utf8mb3_bin         DEFAULT NULL,
    `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin         DEFAULT NULL,
    `SUB_SCOPE_ID_`        varchar(255) COLLATE utf8mb3_bin         DEFAULT NULL,
    `SCOPE_TYPE_`          varchar(255) COLLATE utf8mb3_bin         DEFAULT NULL,
    `TENANT_ID_`           varchar(255) COLLATE utf8mb3_bin         DEFAULT '',
    PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_hi_varinst`
--

DROP TABLE IF EXISTS `act_hi_varinst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_varinst`
(
    `ID_`                varchar(64) COLLATE utf8mb3_bin  NOT NULL,
    `REV_`               int                               DEFAULT '1',
    `PROC_INST_ID_`      varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `EXECUTION_ID_`      varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `TASK_ID_`           varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `NAME_`              varchar(255) COLLATE utf8mb3_bin NOT NULL,
    `VAR_TYPE_`          varchar(100) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SCOPE_ID_`          varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SUB_SCOPE_ID_`      varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SCOPE_TYPE_`        varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `BYTEARRAY_ID_`      varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `DOUBLE_` double DEFAULT NULL,
    `LONG_`              bigint                            DEFAULT NULL,
    `TEXT_`              varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
    `TEXT2_`             varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
    `META_INFO_`         varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
    `CREATE_TIME_`       datetime(3) DEFAULT NULL,
    `LAST_UPDATED_TIME_` datetime(3) DEFAULT NULL,
    PRIMARY KEY (`ID_`),
    KEY                  `ACT_IDX_HI_PROCVAR_NAME_TYPE` (`NAME_`,`VAR_TYPE_`),
    KEY                  `ACT_IDX_HI_VAR_SCOPE_ID_TYPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
    KEY                  `ACT_IDX_HI_VAR_SUB_ID_TYPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
    KEY                  `ACT_IDX_HI_PROCVAR_PROC_INST` (`PROC_INST_ID_`),
    KEY                  `ACT_IDX_HI_PROCVAR_TASK_ID` (`TASK_ID_`),
    KEY                  `ACT_IDX_HI_PROCVAR_EXE` (`EXECUTION_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_id_bytearray`
--

DROP TABLE IF EXISTS `act_id_bytearray`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_bytearray`
(
    `ID_`    varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `REV_`   int                              DEFAULT NULL,
    `NAME_`  varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `BYTES_` longblob,
    PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_id_group`
--

DROP TABLE IF EXISTS `act_id_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_group`
(
    `ID_`   varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `REV_`  int                              DEFAULT NULL,
    `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_id_info`
--

DROP TABLE IF EXISTS `act_id_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_info`
(
    `ID_`        varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `REV_`       int                              DEFAULT NULL,
    `USER_ID_`   varchar(64) COLLATE utf8mb3_bin  DEFAULT NULL,
    `TYPE_`      varchar(64) COLLATE utf8mb3_bin  DEFAULT NULL,
    `KEY_`       varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `VALUE_`     varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `PASSWORD_`  longblob,
    `PARENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_id_membership`
--

DROP TABLE IF EXISTS `act_id_membership`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_membership`
(
    `USER_ID_`  varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `GROUP_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
    PRIMARY KEY (`USER_ID_`, `GROUP_ID_`),
    KEY         `ACT_FK_MEMB_GROUP` (`GROUP_ID_`),
    CONSTRAINT `ACT_FK_MEMB_GROUP` FOREIGN KEY (`GROUP_ID_`) REFERENCES `act_id_group` (`ID_`),
    CONSTRAINT `ACT_FK_MEMB_USER` FOREIGN KEY (`USER_ID_`) REFERENCES `act_id_user` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_id_priv`
--

DROP TABLE IF EXISTS `act_id_priv`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_priv`
(
    `ID_`   varchar(64) COLLATE utf8mb3_bin  NOT NULL,
    `NAME_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
    PRIMARY KEY (`ID_`),
    UNIQUE KEY `ACT_UNIQ_PRIV_NAME` (`NAME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_id_priv_mapping`
--

DROP TABLE IF EXISTS `act_id_priv_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_priv_mapping`
(
    `ID_`       varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `PRIV_ID_`  varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `USER_ID_`  varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `GROUP_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    PRIMARY KEY (`ID_`),
    KEY         `ACT_FK_PRIV_MAPPING` (`PRIV_ID_`),
    KEY         `ACT_IDX_PRIV_USER` (`USER_ID_`),
    KEY         `ACT_IDX_PRIV_GROUP` (`GROUP_ID_`),
    CONSTRAINT `ACT_FK_PRIV_MAPPING` FOREIGN KEY (`PRIV_ID_`) REFERENCES `act_id_priv` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_id_property`
--

DROP TABLE IF EXISTS `act_id_property`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_property`
(
    `NAME_`  varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `VALUE_` varchar(300) COLLATE utf8mb3_bin DEFAULT NULL,
    `REV_`   int                              DEFAULT NULL,
    PRIMARY KEY (`NAME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_id_token`
--

DROP TABLE IF EXISTS `act_id_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_token`
(
    `ID_`          varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `REV_`         int                                      DEFAULT NULL,
    `TOKEN_VALUE_` varchar(255) COLLATE utf8mb3_bin         DEFAULT NULL,
    `TOKEN_DATE_`  timestamp(3)                    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `IP_ADDRESS_`  varchar(255) COLLATE utf8mb3_bin         DEFAULT NULL,
    `USER_AGENT_`  varchar(255) COLLATE utf8mb3_bin         DEFAULT NULL,
    `USER_ID_`     varchar(255) COLLATE utf8mb3_bin         DEFAULT NULL,
    `TOKEN_DATA_`  varchar(2000) COLLATE utf8mb3_bin        DEFAULT NULL,
    PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_id_user`
--

DROP TABLE IF EXISTS `act_id_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_user`
(
    `ID_`           varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `REV_`          int                              DEFAULT NULL,
    `FIRST_`        varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `LAST_`         varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `DISPLAY_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `EMAIL_`        varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `PWD_`          varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `PICTURE_ID_`   varchar(64) COLLATE utf8mb3_bin  DEFAULT NULL,
    `TENANT_ID_`    varchar(255) COLLATE utf8mb3_bin DEFAULT '',
    PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_procdef_info`
--

DROP TABLE IF EXISTS `act_procdef_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_procdef_info`
(
    `ID_`           varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `PROC_DEF_ID_`  varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `REV_`          int                             DEFAULT NULL,
    `INFO_JSON_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
    PRIMARY KEY (`ID_`),
    UNIQUE KEY `ACT_UNIQ_INFO_PROCDEF` (`PROC_DEF_ID_`),
    KEY             `ACT_IDX_INFO_PROCDEF` (`PROC_DEF_ID_`),
    KEY             `ACT_FK_INFO_JSON_BA` (`INFO_JSON_ID_`),
    CONSTRAINT `ACT_FK_INFO_JSON_BA` FOREIGN KEY (`INFO_JSON_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
    CONSTRAINT `ACT_FK_INFO_PROCDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_re_deployment`
--

DROP TABLE IF EXISTS `act_re_deployment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_re_deployment`
(
    `ID_`                   varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `NAME_`                 varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `CATEGORY_`             varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `KEY_`                  varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `TENANT_ID_`            varchar(255) COLLATE utf8mb3_bin DEFAULT '',
    `DEPLOY_TIME_`          timestamp(3) NULL DEFAULT NULL,
    `DERIVED_FROM_`         varchar(64) COLLATE utf8mb3_bin  DEFAULT NULL,
    `DERIVED_FROM_ROOT_`    varchar(64) COLLATE utf8mb3_bin  DEFAULT NULL,
    `PARENT_DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `ENGINE_VERSION_`       varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_re_model`
--

DROP TABLE IF EXISTS `act_re_model`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_re_model`
(
    `ID_`                           varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `REV_`                          int                               DEFAULT NULL,
    `NAME_`                         varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `KEY_`                          varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `CATEGORY_`                     varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `CREATE_TIME_`                  timestamp(3) NULL DEFAULT NULL,
    `LAST_UPDATE_TIME_`             timestamp(3) NULL DEFAULT NULL,
    `VERSION_`                      int                               DEFAULT NULL,
    `META_INFO_`                    varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
    `DEPLOYMENT_ID_`                varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `EDITOR_SOURCE_VALUE_ID_`       varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `EDITOR_SOURCE_EXTRA_VALUE_ID_` varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `TENANT_ID_`                    varchar(255) COLLATE utf8mb3_bin  DEFAULT '',
    PRIMARY KEY (`ID_`),
    KEY                             `ACT_FK_MODEL_SOURCE` (`EDITOR_SOURCE_VALUE_ID_`),
    KEY                             `ACT_FK_MODEL_SOURCE_EXTRA` (`EDITOR_SOURCE_EXTRA_VALUE_ID_`),
    KEY                             `ACT_FK_MODEL_DEPLOYMENT` (`DEPLOYMENT_ID_`),
    CONSTRAINT `ACT_FK_MODEL_DEPLOYMENT` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `act_re_deployment` (`ID_`),
    CONSTRAINT `ACT_FK_MODEL_SOURCE` FOREIGN KEY (`EDITOR_SOURCE_VALUE_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
    CONSTRAINT `ACT_FK_MODEL_SOURCE_EXTRA` FOREIGN KEY (`EDITOR_SOURCE_EXTRA_VALUE_ID_`) REFERENCES `act_ge_bytearray` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_re_procdef`
--

DROP TABLE IF EXISTS `act_re_procdef`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_re_procdef`
(
    `ID_`                     varchar(64) COLLATE utf8mb3_bin  NOT NULL,
    `REV_`                    int                                       DEFAULT NULL,
    `CATEGORY_`               varchar(255) COLLATE utf8mb3_bin          DEFAULT NULL,
    `NAME_`                   varchar(255) COLLATE utf8mb3_bin          DEFAULT NULL,
    `KEY_`                    varchar(255) COLLATE utf8mb3_bin NOT NULL,
    `VERSION_`                int                              NOT NULL,
    `DEPLOYMENT_ID_`          varchar(64) COLLATE utf8mb3_bin           DEFAULT NULL,
    `RESOURCE_NAME_`          varchar(4000) COLLATE utf8mb3_bin         DEFAULT NULL,
    `DGRM_RESOURCE_NAME_`     varchar(4000) COLLATE utf8mb3_bin         DEFAULT NULL,
    `DESCRIPTION_`            varchar(4000) COLLATE utf8mb3_bin         DEFAULT NULL,
    `HAS_START_FORM_KEY_`     tinyint                                   DEFAULT NULL,
    `HAS_GRAPHICAL_NOTATION_` tinyint                                   DEFAULT NULL,
    `SUSPENSION_STATE_`       int                                       DEFAULT NULL,
    `TENANT_ID_`              varchar(255) COLLATE utf8mb3_bin          DEFAULT '',
    `ENGINE_VERSION_`         varchar(255) COLLATE utf8mb3_bin          DEFAULT NULL,
    `DERIVED_FROM_`           varchar(64) COLLATE utf8mb3_bin           DEFAULT NULL,
    `DERIVED_FROM_ROOT_`      varchar(64) COLLATE utf8mb3_bin           DEFAULT NULL,
    `DERIVED_VERSION_`        int                              NOT NULL DEFAULT '0',
    PRIMARY KEY (`ID_`),
    UNIQUE KEY `ACT_UNIQ_PROCDEF` (`KEY_`,`VERSION_`,`DERIVED_VERSION_`,`TENANT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_ru_actinst`
--

DROP TABLE IF EXISTS `act_ru_actinst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_actinst`
(
    `ID_`                varchar(64) COLLATE utf8mb3_bin  NOT NULL,
    `REV_`               int                               DEFAULT '1',
    `PROC_DEF_ID_`       varchar(64) COLLATE utf8mb3_bin  NOT NULL,
    `PROC_INST_ID_`      varchar(64) COLLATE utf8mb3_bin  NOT NULL,
    `EXECUTION_ID_`      varchar(64) COLLATE utf8mb3_bin  NOT NULL,
    `ACT_ID_`            varchar(255) COLLATE utf8mb3_bin NOT NULL,
    `TASK_ID_`           varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `CALL_PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `ACT_NAME_`          varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `ACT_TYPE_`          varchar(255) COLLATE utf8mb3_bin NOT NULL,
    `ASSIGNEE_`          varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `START_TIME_`        datetime(3) NOT NULL,
    `END_TIME_`          datetime(3) DEFAULT NULL,
    `DURATION_`          bigint                            DEFAULT NULL,
    `TRANSACTION_ORDER_` int                               DEFAULT NULL,
    `DELETE_REASON_`     varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
    `TENANT_ID_`         varchar(255) COLLATE utf8mb3_bin  DEFAULT '',
    PRIMARY KEY (`ID_`),
    KEY                  `ACT_IDX_RU_ACTI_START` (`START_TIME_`),
    KEY                  `ACT_IDX_RU_ACTI_END` (`END_TIME_`),
    KEY                  `ACT_IDX_RU_ACTI_PROC` (`PROC_INST_ID_`),
    KEY                  `ACT_IDX_RU_ACTI_PROC_ACT` (`PROC_INST_ID_`,`ACT_ID_`),
    KEY                  `ACT_IDX_RU_ACTI_EXEC` (`EXECUTION_ID_`),
    KEY                  `ACT_IDX_RU_ACTI_EXEC_ACT` (`EXECUTION_ID_`,`ACT_ID_`),
    KEY                  `ACT_IDX_RU_ACTI_TASK` (`TASK_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_ru_deadletter_job`
--

DROP TABLE IF EXISTS `act_ru_deadletter_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_deadletter_job`
(
    `ID_`                  varchar(64) COLLATE utf8mb3_bin  NOT NULL,
    `REV_`                 int                               DEFAULT NULL,
    `CATEGORY_`            varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `TYPE_`                varchar(255) COLLATE utf8mb3_bin NOT NULL,
    `EXCLUSIVE_`           tinyint(1) DEFAULT NULL,
    `EXECUTION_ID_`        varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `PROC_DEF_ID_`         varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `ELEMENT_ID_`          varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `ELEMENT_NAME_`        varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SCOPE_ID_`            varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SUB_SCOPE_ID_`        varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SCOPE_TYPE_`          varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `CORRELATION_ID_`      varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `EXCEPTION_STACK_ID_`  varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `EXCEPTION_MSG_`       varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
    `DUEDATE_`             timestamp(3) NULL DEFAULT NULL,
    `REPEAT_`              varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `HANDLER_TYPE_`        varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `HANDLER_CFG_`         varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
    `CUSTOM_VALUES_ID_`    varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `CREATE_TIME_`         timestamp(3) NULL DEFAULT NULL,
    `TENANT_ID_`           varchar(255) COLLATE utf8mb3_bin  DEFAULT '',
    PRIMARY KEY (`ID_`),
    KEY                    `ACT_IDX_DEADLETTER_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
    KEY                    `ACT_IDX_DEADLETTER_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
    KEY                    `ACT_IDX_DEADLETTER_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
    KEY                    `ACT_IDX_DJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
    KEY                    `ACT_IDX_DJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
    KEY                    `ACT_IDX_DJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
    KEY                    `ACT_FK_DEADLETTER_JOB_EXECUTION` (`EXECUTION_ID_`),
    KEY                    `ACT_FK_DEADLETTER_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
    KEY                    `ACT_FK_DEADLETTER_JOB_PROC_DEF` (`PROC_DEF_ID_`),
    CONSTRAINT `ACT_FK_DEADLETTER_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
    CONSTRAINT `ACT_FK_DEADLETTER_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
    CONSTRAINT `ACT_FK_DEADLETTER_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`),
    CONSTRAINT `ACT_FK_DEADLETTER_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
    CONSTRAINT `ACT_FK_DEADLETTER_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_ru_entitylink`
--

DROP TABLE IF EXISTS `act_ru_entitylink`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_entitylink`
(
    `ID_`                      varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `REV_`                     int                              DEFAULT NULL,
    `CREATE_TIME_`             datetime(3) DEFAULT NULL,
    `LINK_TYPE_`               varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `SCOPE_ID_`                varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `SUB_SCOPE_ID_`            varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `SCOPE_TYPE_`              varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `SCOPE_DEFINITION_ID_`     varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `PARENT_ELEMENT_ID_`       varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `REF_SCOPE_ID_`            varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `REF_SCOPE_TYPE_`          varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `REF_SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `ROOT_SCOPE_ID_`           varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `ROOT_SCOPE_TYPE_`         varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `HIERARCHY_TYPE_`          varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    PRIMARY KEY (`ID_`),
    KEY                        `ACT_IDX_ENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`),
    KEY                        `ACT_IDX_ENT_LNK_REF_SCOPE` (`REF_SCOPE_ID_`,`REF_SCOPE_TYPE_`,`LINK_TYPE_`),
    KEY                        `ACT_IDX_ENT_LNK_ROOT_SCOPE` (`ROOT_SCOPE_ID_`,`ROOT_SCOPE_TYPE_`,`LINK_TYPE_`),
    KEY                        `ACT_IDX_ENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_ru_event_subscr`
--

DROP TABLE IF EXISTS `act_ru_event_subscr`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_event_subscr`
(
    `ID_`                   varchar(64) COLLATE utf8mb3_bin  NOT NULL,
    `REV_`                  int                                       DEFAULT NULL,
    `EVENT_TYPE_`           varchar(255) COLLATE utf8mb3_bin NOT NULL,
    `EVENT_NAME_`           varchar(255) COLLATE utf8mb3_bin          DEFAULT NULL,
    `EXECUTION_ID_`         varchar(64) COLLATE utf8mb3_bin           DEFAULT NULL,
    `PROC_INST_ID_`         varchar(64) COLLATE utf8mb3_bin           DEFAULT NULL,
    `ACTIVITY_ID_`          varchar(64) COLLATE utf8mb3_bin           DEFAULT NULL,
    `CONFIGURATION_`        varchar(255) COLLATE utf8mb3_bin          DEFAULT NULL,
    `CREATED_`              timestamp(3)                     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `PROC_DEF_ID_`          varchar(64) COLLATE utf8mb3_bin           DEFAULT NULL,
    `SUB_SCOPE_ID_`         varchar(64) COLLATE utf8mb3_bin           DEFAULT NULL,
    `SCOPE_ID_`             varchar(64) COLLATE utf8mb3_bin           DEFAULT NULL,
    `SCOPE_DEFINITION_ID_`  varchar(64) COLLATE utf8mb3_bin           DEFAULT NULL,
    `SCOPE_DEFINITION_KEY_` varchar(255) COLLATE utf8mb3_bin          DEFAULT NULL,
    `SCOPE_TYPE_`           varchar(64) COLLATE utf8mb3_bin           DEFAULT NULL,
    `LOCK_TIME_`            timestamp(3) NULL DEFAULT NULL,
    `LOCK_OWNER_`           varchar(255) COLLATE utf8mb3_bin          DEFAULT NULL,
    `TENANT_ID_`            varchar(255) COLLATE utf8mb3_bin          DEFAULT '',
    PRIMARY KEY (`ID_`),
    KEY                     `ACT_IDX_EVENT_SUBSCR_CONFIG_` (`CONFIGURATION_`),
    KEY                     `ACT_IDX_EVENT_SUBSCR_SCOPEREF_` (`SCOPE_ID_`,`SCOPE_TYPE_`),
    KEY                     `ACT_FK_EVENT_EXEC` (`EXECUTION_ID_`),
    CONSTRAINT `ACT_FK_EVENT_EXEC` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_ru_execution`
--

DROP TABLE IF EXISTS `act_ru_execution`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_execution`
(
    `ID_`                        varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `REV_`                       int                              DEFAULT NULL,
    `PROC_INST_ID_`              varchar(64) COLLATE utf8mb3_bin  DEFAULT NULL,
    `BUSINESS_KEY_`              varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `PARENT_ID_`                 varchar(64) COLLATE utf8mb3_bin  DEFAULT NULL,
    `PROC_DEF_ID_`               varchar(64) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SUPER_EXEC_`                varchar(64) COLLATE utf8mb3_bin  DEFAULT NULL,
    `ROOT_PROC_INST_ID_`         varchar(64) COLLATE utf8mb3_bin  DEFAULT NULL,
    `ACT_ID_`                    varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `IS_ACTIVE_`                 tinyint                          DEFAULT NULL,
    `IS_CONCURRENT_`             tinyint                          DEFAULT NULL,
    `IS_SCOPE_`                  tinyint                          DEFAULT NULL,
    `IS_EVENT_SCOPE_`            tinyint                          DEFAULT NULL,
    `IS_MI_ROOT_`                tinyint                          DEFAULT NULL,
    `SUSPENSION_STATE_`          int                              DEFAULT NULL,
    `CACHED_ENT_STATE_`          int                              DEFAULT NULL,
    `TENANT_ID_`                 varchar(255) COLLATE utf8mb3_bin DEFAULT '',
    `NAME_`                      varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `START_ACT_ID_`              varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `START_TIME_`                datetime(3) DEFAULT NULL,
    `START_USER_ID_`             varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `LOCK_TIME_`                 timestamp(3) NULL DEFAULT NULL,
    `LOCK_OWNER_`                varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `IS_COUNT_ENABLED_`          tinyint                          DEFAULT NULL,
    `EVT_SUBSCR_COUNT_`          int                              DEFAULT NULL,
    `TASK_COUNT_`                int                              DEFAULT NULL,
    `JOB_COUNT_`                 int                              DEFAULT NULL,
    `TIMER_JOB_COUNT_`           int                              DEFAULT NULL,
    `SUSP_JOB_COUNT_`            int                              DEFAULT NULL,
    `DEADLETTER_JOB_COUNT_`      int                              DEFAULT NULL,
    `EXTERNAL_WORKER_JOB_COUNT_` int                              DEFAULT NULL,
    `VAR_COUNT_`                 int                              DEFAULT NULL,
    `ID_LINK_COUNT_`             int                              DEFAULT NULL,
    `CALLBACK_ID_`               varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `CALLBACK_TYPE_`             varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `REFERENCE_ID_`              varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `REFERENCE_TYPE_`            varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `PROPAGATED_STAGE_INST_ID_`  varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `BUSINESS_STATUS_`           varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    PRIMARY KEY (`ID_`),
    KEY                          `ACT_IDX_EXEC_BUSKEY` (`BUSINESS_KEY_`),
    KEY                          `ACT_IDC_EXEC_ROOT` (`ROOT_PROC_INST_ID_`),
    KEY                          `ACT_IDX_EXEC_REF_ID_` (`REFERENCE_ID_`),
    KEY                          `ACT_FK_EXE_PROCINST` (`PROC_INST_ID_`),
    KEY                          `ACT_FK_EXE_PARENT` (`PARENT_ID_`),
    KEY                          `ACT_FK_EXE_SUPER` (`SUPER_EXEC_`),
    KEY                          `ACT_FK_EXE_PROCDEF` (`PROC_DEF_ID_`),
    CONSTRAINT `ACT_FK_EXE_PARENT` FOREIGN KEY (`PARENT_ID_`) REFERENCES `act_ru_execution` (`ID_`) ON DELETE CASCADE,
    CONSTRAINT `ACT_FK_EXE_PROCDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
    CONSTRAINT `ACT_FK_EXE_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `act_ru_execution` (`ID_`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `ACT_FK_EXE_SUPER` FOREIGN KEY (`SUPER_EXEC_`) REFERENCES `act_ru_execution` (`ID_`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_ru_external_job`
--

DROP TABLE IF EXISTS `act_ru_external_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_external_job`
(
    `ID_`                  varchar(64) COLLATE utf8mb3_bin  NOT NULL,
    `REV_`                 int                               DEFAULT NULL,
    `CATEGORY_`            varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `TYPE_`                varchar(255) COLLATE utf8mb3_bin NOT NULL,
    `LOCK_EXP_TIME_`       timestamp(3) NULL DEFAULT NULL,
    `LOCK_OWNER_`          varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `EXCLUSIVE_`           tinyint(1) DEFAULT NULL,
    `EXECUTION_ID_`        varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `PROC_DEF_ID_`         varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `ELEMENT_ID_`          varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `ELEMENT_NAME_`        varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SCOPE_ID_`            varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SUB_SCOPE_ID_`        varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SCOPE_TYPE_`          varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `CORRELATION_ID_`      varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `RETRIES_`             int                               DEFAULT NULL,
    `EXCEPTION_STACK_ID_`  varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `EXCEPTION_MSG_`       varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
    `DUEDATE_`             timestamp(3) NULL DEFAULT NULL,
    `REPEAT_`              varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `HANDLER_TYPE_`        varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `HANDLER_CFG_`         varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
    `CUSTOM_VALUES_ID_`    varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `CREATE_TIME_`         timestamp(3) NULL DEFAULT NULL,
    `TENANT_ID_`           varchar(255) COLLATE utf8mb3_bin  DEFAULT '',
    PRIMARY KEY (`ID_`),
    KEY                    `ACT_IDX_EXTERNAL_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
    KEY                    `ACT_IDX_EXTERNAL_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
    KEY                    `ACT_IDX_EXTERNAL_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
    KEY                    `ACT_IDX_EJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
    KEY                    `ACT_IDX_EJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
    KEY                    `ACT_IDX_EJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
    CONSTRAINT `ACT_FK_EXTERNAL_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
    CONSTRAINT `ACT_FK_EXTERNAL_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `act_ge_bytearray` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_ru_history_job`
--

DROP TABLE IF EXISTS `act_ru_history_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_history_job`
(
    `ID_`                 varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `REV_`                int                               DEFAULT NULL,
    `LOCK_EXP_TIME_`      timestamp(3) NULL DEFAULT NULL,
    `LOCK_OWNER_`         varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `RETRIES_`            int                               DEFAULT NULL,
    `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `EXCEPTION_MSG_`      varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
    `HANDLER_TYPE_`       varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `HANDLER_CFG_`        varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
    `CUSTOM_VALUES_ID_`   varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `ADV_HANDLER_CFG_ID_` varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `CREATE_TIME_`        timestamp(3) NULL DEFAULT NULL,
    `SCOPE_TYPE_`         varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `TENANT_ID_`          varchar(255) COLLATE utf8mb3_bin  DEFAULT '',
    PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_ru_identitylink`
--

DROP TABLE IF EXISTS `act_ru_identitylink`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_identitylink`
(
    `ID_`                  varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `REV_`                 int                              DEFAULT NULL,
    `GROUP_ID_`            varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `TYPE_`                varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `USER_ID_`             varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `TASK_ID_`             varchar(64) COLLATE utf8mb3_bin  DEFAULT NULL,
    `PROC_INST_ID_`        varchar(64) COLLATE utf8mb3_bin  DEFAULT NULL,
    `PROC_DEF_ID_`         varchar(64) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SCOPE_ID_`            varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `SUB_SCOPE_ID_`        varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `SCOPE_TYPE_`          varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    PRIMARY KEY (`ID_`),
    KEY                    `ACT_IDX_IDENT_LNK_USER` (`USER_ID_`),
    KEY                    `ACT_IDX_IDENT_LNK_GROUP` (`GROUP_ID_`),
    KEY                    `ACT_IDX_IDENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
    KEY                    `ACT_IDX_IDENT_LNK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
    KEY                    `ACT_IDX_IDENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
    KEY                    `ACT_IDX_ATHRZ_PROCEDEF` (`PROC_DEF_ID_`),
    KEY                    `ACT_FK_TSKASS_TASK` (`TASK_ID_`),
    KEY                    `ACT_FK_IDL_PROCINST` (`PROC_INST_ID_`),
    CONSTRAINT `ACT_FK_ATHRZ_PROCEDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
    CONSTRAINT `ACT_FK_IDL_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `act_ru_execution` (`ID_`),
    CONSTRAINT `ACT_FK_TSKASS_TASK` FOREIGN KEY (`TASK_ID_`) REFERENCES `act_ru_task` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_ru_job`
--

DROP TABLE IF EXISTS `act_ru_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_job`
(
    `ID_`                  varchar(64) COLLATE utf8mb3_bin  NOT NULL,
    `REV_`                 int                               DEFAULT NULL,
    `CATEGORY_`            varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `TYPE_`                varchar(255) COLLATE utf8mb3_bin NOT NULL,
    `LOCK_EXP_TIME_`       timestamp(3) NULL DEFAULT NULL,
    `LOCK_OWNER_`          varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `EXCLUSIVE_`           tinyint(1) DEFAULT NULL,
    `EXECUTION_ID_`        varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `PROC_DEF_ID_`         varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `ELEMENT_ID_`          varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `ELEMENT_NAME_`        varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SCOPE_ID_`            varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SUB_SCOPE_ID_`        varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SCOPE_TYPE_`          varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `CORRELATION_ID_`      varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `RETRIES_`             int                               DEFAULT NULL,
    `EXCEPTION_STACK_ID_`  varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `EXCEPTION_MSG_`       varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
    `DUEDATE_`             timestamp(3) NULL DEFAULT NULL,
    `REPEAT_`              varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `HANDLER_TYPE_`        varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `HANDLER_CFG_`         varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
    `CUSTOM_VALUES_ID_`    varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `CREATE_TIME_`         timestamp(3) NULL DEFAULT NULL,
    `TENANT_ID_`           varchar(255) COLLATE utf8mb3_bin  DEFAULT '',
    PRIMARY KEY (`ID_`),
    KEY                    `ACT_IDX_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
    KEY                    `ACT_IDX_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
    KEY                    `ACT_IDX_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
    KEY                    `ACT_IDX_JOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
    KEY                    `ACT_IDX_JOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
    KEY                    `ACT_IDX_JOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
    KEY                    `ACT_FK_JOB_EXECUTION` (`EXECUTION_ID_`),
    KEY                    `ACT_FK_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
    KEY                    `ACT_FK_JOB_PROC_DEF` (`PROC_DEF_ID_`),
    CONSTRAINT `ACT_FK_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
    CONSTRAINT `ACT_FK_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
    CONSTRAINT `ACT_FK_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`),
    CONSTRAINT `ACT_FK_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
    CONSTRAINT `ACT_FK_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_ru_suspended_job`
--

DROP TABLE IF EXISTS `act_ru_suspended_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_suspended_job`
(
    `ID_`                  varchar(64) COLLATE utf8mb3_bin  NOT NULL,
    `REV_`                 int                               DEFAULT NULL,
    `CATEGORY_`            varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `TYPE_`                varchar(255) COLLATE utf8mb3_bin NOT NULL,
    `EXCLUSIVE_`           tinyint(1) DEFAULT NULL,
    `EXECUTION_ID_`        varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `PROC_DEF_ID_`         varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `ELEMENT_ID_`          varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `ELEMENT_NAME_`        varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SCOPE_ID_`            varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SUB_SCOPE_ID_`        varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SCOPE_TYPE_`          varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `CORRELATION_ID_`      varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `RETRIES_`             int                               DEFAULT NULL,
    `EXCEPTION_STACK_ID_`  varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `EXCEPTION_MSG_`       varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
    `DUEDATE_`             timestamp(3) NULL DEFAULT NULL,
    `REPEAT_`              varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `HANDLER_TYPE_`        varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `HANDLER_CFG_`         varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
    `CUSTOM_VALUES_ID_`    varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `CREATE_TIME_`         timestamp(3) NULL DEFAULT NULL,
    `TENANT_ID_`           varchar(255) COLLATE utf8mb3_bin  DEFAULT '',
    PRIMARY KEY (`ID_`),
    KEY                    `ACT_IDX_SUSPENDED_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
    KEY                    `ACT_IDX_SUSPENDED_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
    KEY                    `ACT_IDX_SUSPENDED_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
    KEY                    `ACT_IDX_SJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
    KEY                    `ACT_IDX_SJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
    KEY                    `ACT_IDX_SJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
    KEY                    `ACT_FK_SUSPENDED_JOB_EXECUTION` (`EXECUTION_ID_`),
    KEY                    `ACT_FK_SUSPENDED_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
    KEY                    `ACT_FK_SUSPENDED_JOB_PROC_DEF` (`PROC_DEF_ID_`),
    CONSTRAINT `ACT_FK_SUSPENDED_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
    CONSTRAINT `ACT_FK_SUSPENDED_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
    CONSTRAINT `ACT_FK_SUSPENDED_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`),
    CONSTRAINT `ACT_FK_SUSPENDED_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
    CONSTRAINT `ACT_FK_SUSPENDED_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_ru_task`
--

DROP TABLE IF EXISTS `act_ru_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_task`
(
    `ID_`                       varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `REV_`                      int                               DEFAULT NULL,
    `EXECUTION_ID_`             varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `PROC_INST_ID_`             varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `PROC_DEF_ID_`              varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `TASK_DEF_ID_`              varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `SCOPE_ID_`                 varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SUB_SCOPE_ID_`             varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SCOPE_TYPE_`               varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SCOPE_DEFINITION_ID_`      varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `STATE_`                    varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `NAME_`                     varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `PARENT_TASK_ID_`           varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `DESCRIPTION_`              varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
    `TASK_DEF_KEY_`             varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `OWNER_`                    varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `ASSIGNEE_`                 varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `DELEGATION_`               varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `PRIORITY_`                 int                               DEFAULT NULL,
    `CREATE_TIME_`              timestamp(3) NULL DEFAULT NULL,
    `IN_PROGRESS_TIME_`         datetime(3) DEFAULT NULL,
    `IN_PROGRESS_STARTED_BY_`   varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `CLAIM_TIME_`               datetime(3) DEFAULT NULL,
    `CLAIMED_BY_`               varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SUSPENDED_TIME_`           datetime(3) DEFAULT NULL,
    `SUSPENDED_BY_`             varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `IN_PROGRESS_DUE_DATE_`     datetime(3) DEFAULT NULL,
    `DUE_DATE_`                 datetime(3) DEFAULT NULL,
    `CATEGORY_`                 varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SUSPENSION_STATE_`         int                               DEFAULT NULL,
    `TENANT_ID_`                varchar(255) COLLATE utf8mb3_bin  DEFAULT '',
    `FORM_KEY_`                 varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `IS_COUNT_ENABLED_`         tinyint                           DEFAULT NULL,
    `VAR_COUNT_`                int                               DEFAULT NULL,
    `ID_LINK_COUNT_`            int                               DEFAULT NULL,
    `SUB_TASK_COUNT_`           int                               DEFAULT NULL,
    PRIMARY KEY (`ID_`),
    KEY                         `ACT_IDX_TASK_CREATE` (`CREATE_TIME_`),
    KEY                         `ACT_IDX_TASK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
    KEY                         `ACT_IDX_TASK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
    KEY                         `ACT_IDX_TASK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
    KEY                         `ACT_FK_TASK_EXE` (`EXECUTION_ID_`),
    KEY                         `ACT_FK_TASK_PROCINST` (`PROC_INST_ID_`),
    KEY                         `ACT_FK_TASK_PROCDEF` (`PROC_DEF_ID_`),
    CONSTRAINT `ACT_FK_TASK_EXE` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`),
    CONSTRAINT `ACT_FK_TASK_PROCDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
    CONSTRAINT `ACT_FK_TASK_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_ru_timer_job`
--

DROP TABLE IF EXISTS `act_ru_timer_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_timer_job`
(
    `ID_`                  varchar(64) COLLATE utf8mb3_bin  NOT NULL,
    `REV_`                 int                               DEFAULT NULL,
    `CATEGORY_`            varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `TYPE_`                varchar(255) COLLATE utf8mb3_bin NOT NULL,
    `LOCK_EXP_TIME_`       timestamp(3) NULL DEFAULT NULL,
    `LOCK_OWNER_`          varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `EXCLUSIVE_`           tinyint(1) DEFAULT NULL,
    `EXECUTION_ID_`        varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `PROC_DEF_ID_`         varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `ELEMENT_ID_`          varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `ELEMENT_NAME_`        varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SCOPE_ID_`            varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SUB_SCOPE_ID_`        varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SCOPE_TYPE_`          varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `CORRELATION_ID_`      varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `RETRIES_`             int                               DEFAULT NULL,
    `EXCEPTION_STACK_ID_`  varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `EXCEPTION_MSG_`       varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
    `DUEDATE_`             timestamp(3) NULL DEFAULT NULL,
    `REPEAT_`              varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `HANDLER_TYPE_`        varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `HANDLER_CFG_`         varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
    `CUSTOM_VALUES_ID_`    varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `CREATE_TIME_`         timestamp(3) NULL DEFAULT NULL,
    `TENANT_ID_`           varchar(255) COLLATE utf8mb3_bin  DEFAULT '',
    PRIMARY KEY (`ID_`),
    KEY                    `ACT_IDX_TIMER_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
    KEY                    `ACT_IDX_TIMER_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
    KEY                    `ACT_IDX_TIMER_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
    KEY                    `ACT_IDX_TIMER_JOB_DUEDATE` (`DUEDATE_`),
    KEY                    `ACT_IDX_TJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
    KEY                    `ACT_IDX_TJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
    KEY                    `ACT_IDX_TJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
    KEY                    `ACT_FK_TIMER_JOB_EXECUTION` (`EXECUTION_ID_`),
    KEY                    `ACT_FK_TIMER_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
    KEY                    `ACT_FK_TIMER_JOB_PROC_DEF` (`PROC_DEF_ID_`),
    CONSTRAINT `ACT_FK_TIMER_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
    CONSTRAINT `ACT_FK_TIMER_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
    CONSTRAINT `ACT_FK_TIMER_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`),
    CONSTRAINT `ACT_FK_TIMER_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
    CONSTRAINT `ACT_FK_TIMER_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_ru_variable`
--

DROP TABLE IF EXISTS `act_ru_variable`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_variable`
(
    `ID_`           varchar(64) COLLATE utf8mb3_bin  NOT NULL,
    `REV_`          int                               DEFAULT NULL,
    `TYPE_`         varchar(255) COLLATE utf8mb3_bin NOT NULL,
    `NAME_`         varchar(255) COLLATE utf8mb3_bin NOT NULL,
    `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `TASK_ID_`      varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `SCOPE_ID_`     varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SCOPE_TYPE_`   varchar(255) COLLATE utf8mb3_bin  DEFAULT NULL,
    `BYTEARRAY_ID_` varchar(64) COLLATE utf8mb3_bin   DEFAULT NULL,
    `DOUBLE_` double DEFAULT NULL,
    `LONG_`         bigint                            DEFAULT NULL,
    `TEXT_`         varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
    `TEXT2_`        varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
    `META_INFO_`    varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
    PRIMARY KEY (`ID_`),
    KEY             `ACT_IDX_RU_VAR_SCOPE_ID_TYPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
    KEY             `ACT_IDX_RU_VAR_SUB_ID_TYPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
    KEY             `ACT_FK_VAR_BYTEARRAY` (`BYTEARRAY_ID_`),
    KEY             `ACT_IDX_VARIABLE_TASK_ID` (`TASK_ID_`),
    KEY             `ACT_FK_VAR_EXE` (`EXECUTION_ID_`),
    KEY             `ACT_FK_VAR_PROCINST` (`PROC_INST_ID_`),
    CONSTRAINT `ACT_FK_VAR_BYTEARRAY` FOREIGN KEY (`BYTEARRAY_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
    CONSTRAINT `ACT_FK_VAR_EXE` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`),
    CONSTRAINT `ACT_FK_VAR_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `biz_leave_request`
--

DROP TABLE IF EXISTS `biz_leave_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_leave_request`
(
    `id`                  varchar(64)    NOT NULL COMMENT '主键ID',
    `business_key`        varchar(64)    NOT NULL COMMENT '业务Key（关联流程）',
    `process_instance_id` varchar(64)             DEFAULT NULL COMMENT '流程实例ID',
    `apply_user_id`       varchar(64)    NOT NULL COMMENT '申请人ID',
    `apply_user_name`     varchar(100)   NOT NULL COMMENT '申请人姓名',
    `apply_dept_id`       varchar(64)             DEFAULT NULL COMMENT '申请部门ID',
    `apply_dept_name`     varchar(200)            DEFAULT NULL COMMENT '申请部门名称',
    `leave_type`          varchar(20)    NOT NULL COMMENT '请假类型：annual-年假/sick-病假/personal-事假/marriage-婚假/maternity-产假',
    `start_time`          datetime       NOT NULL COMMENT '开始时间',
    `end_time`            datetime       NOT NULL COMMENT '结束时间',
    `duration`            decimal(10, 1) NOT NULL COMMENT '请假天数',
    `reason`              text COMMENT '请假原因',
    `attachments`         text COMMENT '附件JSON列表',
    `status`              varchar(20)    NOT NULL DEFAULT 'draft' COMMENT '状态：draft-草稿/pending-审批中/approved-已通过/rejected-已驳回/canceled-已取消',
    `approve_user_id`     varchar(64)             DEFAULT NULL COMMENT '审批人ID',
    `approve_user_name`   varchar(100)            DEFAULT NULL COMMENT '审批人姓名',
    `approve_time`        datetime                DEFAULT NULL COMMENT '审批时间',
    `approve_comment`     text COMMENT '审批意见',
    `approve_attachments` text COMMENT '审批附件JSON',
    `create_time`         datetime                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         datetime                DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`             tinyint(1) DEFAULT '0' COMMENT '删除标记',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_business_key` (`business_key`),
    KEY                   `idx_apply_user` (`apply_user_id`),
    KEY                   `idx_process_instance` (`process_instance_id`),
    KEY                   `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='请假申请表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `config_properties`
--

DROP TABLE IF EXISTS `config_properties`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `config_properties`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `key`         varchar(200) NOT NULL COMMENT '配置键',
    `value`       text COMMENT '配置值',
    `description` varchar(500) DEFAULT NULL COMMENT '描述',
    `group`       varchar(100) DEFAULT 'DEFAULT_GROUP' COMMENT '配置分组',
    `type`        varchar(50)  DEFAULT 'STRING' COMMENT '值类型(STRING/NUMBER/BOOLEAN/JSON)',
    `enabled`     tinyint(1) DEFAULT '1' COMMENT '是否启用',
    `create_time` datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   varchar(64)  DEFAULT NULL COMMENT '创建人',
    `update_by`   varchar(64)  DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_key` (`key`),
    KEY           `idx_group` (`group`),
    KEY           `idx_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='配置属性表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flw_channel_definition`
--

DROP TABLE IF EXISTS `flw_channel_definition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flw_channel_definition`
(
    `ID_`             varchar(255) NOT NULL,
    `NAME_`           varchar(255) DEFAULT NULL,
    `VERSION_`        int          DEFAULT NULL,
    `KEY_`            varchar(255) DEFAULT NULL,
    `CATEGORY_`       varchar(255) DEFAULT NULL,
    `DEPLOYMENT_ID_`  varchar(255) DEFAULT NULL,
    `CREATE_TIME_`    datetime(3) DEFAULT NULL,
    `TENANT_ID_`      varchar(255) DEFAULT NULL,
    `RESOURCE_NAME_`  varchar(255) DEFAULT NULL,
    `DESCRIPTION_`    varchar(255) DEFAULT NULL,
    `TYPE_`           varchar(255) DEFAULT NULL,
    `IMPLEMENTATION_` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`ID_`),
    UNIQUE KEY `ACT_IDX_CHANNEL_DEF_UNIQ` (`KEY_`,`VERSION_`,`TENANT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flw_ev_databasechangelog`
--

DROP TABLE IF EXISTS `flw_ev_databasechangelog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flw_ev_databasechangelog`
(
    `ID`            varchar(255) NOT NULL,
    `AUTHOR`        varchar(255) NOT NULL,
    `FILENAME`      varchar(255) NOT NULL,
    `DATEEXECUTED`  datetime     NOT NULL,
    `ORDEREXECUTED` int          NOT NULL,
    `EXECTYPE`      varchar(10)  NOT NULL,
    `MD5SUM`        varchar(35)  DEFAULT NULL,
    `DESCRIPTION`   varchar(255) DEFAULT NULL,
    `COMMENTS`      varchar(255) DEFAULT NULL,
    `TAG`           varchar(255) DEFAULT NULL,
    `LIQUIBASE`     varchar(20)  DEFAULT NULL,
    `CONTEXTS`      varchar(255) DEFAULT NULL,
    `LABELS`        varchar(255) DEFAULT NULL,
    `DEPLOYMENT_ID` varchar(10)  DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flw_ev_databasechangeloglock`
--

DROP TABLE IF EXISTS `flw_ev_databasechangeloglock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flw_ev_databasechangeloglock`
(
    `ID`          int NOT NULL,
    `LOCKED`      tinyint(1) NOT NULL,
    `LOCKGRANTED` datetime     DEFAULT NULL,
    `LOCKEDBY`    varchar(255) DEFAULT NULL,
    PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flw_event_definition`
--

DROP TABLE IF EXISTS `flw_event_definition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flw_event_definition`
(
    `ID_`            varchar(255) NOT NULL,
    `NAME_`          varchar(255) DEFAULT NULL,
    `VERSION_`       int          DEFAULT NULL,
    `KEY_`           varchar(255) DEFAULT NULL,
    `CATEGORY_`      varchar(255) DEFAULT NULL,
    `DEPLOYMENT_ID_` varchar(255) DEFAULT NULL,
    `TENANT_ID_`     varchar(255) DEFAULT NULL,
    `RESOURCE_NAME_` varchar(255) DEFAULT NULL,
    `DESCRIPTION_`   varchar(255) DEFAULT NULL,
    PRIMARY KEY (`ID_`),
    UNIQUE KEY `ACT_IDX_EVENT_DEF_UNIQ` (`KEY_`,`VERSION_`,`TENANT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flw_event_deployment`
--

DROP TABLE IF EXISTS `flw_event_deployment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flw_event_deployment`
(
    `ID_`                   varchar(255) NOT NULL,
    `NAME_`                 varchar(255) DEFAULT NULL,
    `CATEGORY_`             varchar(255) DEFAULT NULL,
    `DEPLOY_TIME_`          datetime(3) DEFAULT NULL,
    `TENANT_ID_`            varchar(255) DEFAULT NULL,
    `PARENT_DEPLOYMENT_ID_` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flw_event_resource`
--

DROP TABLE IF EXISTS `flw_event_resource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flw_event_resource`
(
    `ID_`             varchar(255) NOT NULL,
    `NAME_`           varchar(255) DEFAULT NULL,
    `DEPLOYMENT_ID_`  varchar(255) DEFAULT NULL,
    `RESOURCE_BYTES_` longblob,
    PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flw_ru_batch`
--

DROP TABLE IF EXISTS `flw_ru_batch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flw_ru_batch`
(
    `ID_`            varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `REV_`           int                              DEFAULT NULL,
    `TYPE_`          varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `SEARCH_KEY_`    varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `SEARCH_KEY2_`   varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `CREATE_TIME_`   datetime(3) NOT NULL,
    `COMPLETE_TIME_` datetime(3) DEFAULT NULL,
    `STATUS_`        varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `BATCH_DOC_ID_`  varchar(64) COLLATE utf8mb3_bin  DEFAULT NULL,
    `TENANT_ID_`     varchar(255) COLLATE utf8mb3_bin DEFAULT '',
    PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flw_ru_batch_part`
--

DROP TABLE IF EXISTS `flw_ru_batch_part`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flw_ru_batch_part`
(
    `ID_`            varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `REV_`           int                              DEFAULT NULL,
    `BATCH_ID_`      varchar(64) COLLATE utf8mb3_bin  DEFAULT NULL,
    `TYPE_`          varchar(64) COLLATE utf8mb3_bin NOT NULL,
    `SCOPE_ID_`      varchar(64) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SUB_SCOPE_ID_`  varchar(64) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SCOPE_TYPE_`    varchar(64) COLLATE utf8mb3_bin  DEFAULT NULL,
    `SEARCH_KEY_`    varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `SEARCH_KEY2_`   varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `CREATE_TIME_`   datetime(3) NOT NULL,
    `COMPLETE_TIME_` datetime(3) DEFAULT NULL,
    `STATUS_`        varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
    `RESULT_DOC_ID_` varchar(64) COLLATE utf8mb3_bin  DEFAULT NULL,
    `TENANT_ID_`     varchar(255) COLLATE utf8mb3_bin DEFAULT '',
    PRIMARY KEY (`ID_`),
    KEY              `FLW_IDX_BATCH_PART` (`BATCH_ID_`),
    CONSTRAINT `FLW_FK_BATCH_PART_PARENT` FOREIGN KEY (`BATCH_ID_`) REFERENCES `flw_ru_batch` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `gen_datasource`
--

DROP TABLE IF EXISTS `gen_datasource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gen_datasource`
(
    `datasource_id`     bigint       NOT NULL AUTO_INCREMENT COMMENT '数据源ID',
    `datasource_name`   varchar(100) NOT NULL COMMENT '数据源名称',
    `datasource_code`   varchar(50)  NOT NULL COMMENT '数据源编码（唯一标识）',
    `db_type`           varchar(20)  NOT NULL DEFAULT 'MySQL' COMMENT '数据库类型：MySQL/Oracle/PostgreSQL/SQLServer',
    `driver_class_name` varchar(200) NOT NULL COMMENT '驱动类名',
    `url`               varchar(500) NOT NULL COMMENT 'JDBC连接地址',
    `username`          varchar(100) NOT NULL COMMENT '用户名',
    `password`          varchar(200) NOT NULL COMMENT '密码（加密存储）',
    `is_default`        tinyint               DEFAULT '0' COMMENT '是否默认数据源（1-是，0-否）',
    `is_enabled`        tinyint               DEFAULT '1' COMMENT '是否启用（1-启用，0-禁用）',
    `test_query`        varchar(200)          DEFAULT 'SELECT 1' COMMENT '测试查询SQL',
    `sort`              int                   DEFAULT '0' COMMENT '排序',
    `remark`            varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`         varchar(64)           DEFAULT NULL COMMENT '创建者',
    `update_by`         varchar(64)           DEFAULT NULL COMMENT '更新者',
    PRIMARY KEY (`datasource_id`),
    UNIQUE KEY `uk_datasource_code` (`datasource_code`),
    KEY                 `idx_is_default` (`is_default`),
    KEY                 `idx_is_enabled` (`is_enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成器数据源配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `gen_table`
--

DROP TABLE IF EXISTS `gen_table`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gen_table`
(
    `table_id`        bigint       NOT NULL AUTO_INCREMENT COMMENT '表ID',
    `datasource_id`   bigint unsigned DEFAULT NULL COMMENT '数据源ID',
    `table_name`      varchar(200) NOT NULL COMMENT '表名称',
    `table_comment`   varchar(500)          DEFAULT NULL COMMENT '表描述',
    `class_name`      varchar(100) NOT NULL COMMENT '实体类名称',
    `business_name`   varchar(100)          DEFAULT NULL COMMENT '业务名称',
    `function_name`   varchar(100)          DEFAULT NULL COMMENT '功能名称',
    `module_name`     varchar(100)          DEFAULT 'system' COMMENT '模块名称',
    `package_name`    varchar(200)          DEFAULT 'com.mdframe.forge.plugin' COMMENT '包路径',
    `author`          varchar(50)           DEFAULT 'Forge Generator' COMMENT '作者',
    `gen_type`        varchar(20)           DEFAULT 'DOWNLOAD' COMMENT '生成方式：DOWNLOAD-下载代码包/PROJECT-直接生成到项目',
    `gen_path`        varchar(500)          DEFAULT '/' COMMENT '生成路径',
    `template_engine` varchar(20)           DEFAULT 'VELOCITY' COMMENT '模板引擎：VELOCITY/FREEMARKER/AI',
    `options`         json                  DEFAULT NULL COMMENT '其他生成选项',
    `remark`          varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`       varchar(64)           DEFAULT NULL COMMENT '创建者',
    `update_by`       varchar(64)           DEFAULT NULL COMMENT '更新者',
    PRIMARY KEY (`table_id`),
    UNIQUE KEY `uk_table_name` (`table_name`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成表配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `gen_table_column`
--

DROP TABLE IF EXISTS `gen_table_column`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gen_table_column`
(
    `column_id`      bigint       NOT NULL AUTO_INCREMENT COMMENT '字段ID',
    `table_id`       bigint       NOT NULL COMMENT '表ID',
    `column_name`    varchar(200) NOT NULL COMMENT '字段名称',
    `column_comment` varchar(500)          DEFAULT NULL COMMENT '字段描述',
    `column_type`    varchar(100) NOT NULL COMMENT '字段类型',
    `java_type`      varchar(50)           DEFAULT 'String' COMMENT 'Java类型',
    `java_field`     varchar(100)          DEFAULT NULL COMMENT 'Java字段名',
    `is_pk`          tinyint               DEFAULT '0' COMMENT '是否主键',
    `is_increment`   tinyint               DEFAULT '0' COMMENT '是否自增',
    `is_required`    tinyint               DEFAULT '0' COMMENT '是否必填',
    `is_insert`      tinyint               DEFAULT '1' COMMENT '是否插入字段',
    `is_edit`        tinyint               DEFAULT '1' COMMENT '是否编辑字段',
    `is_list`        tinyint               DEFAULT '1' COMMENT '是否列表字段',
    `is_query`       tinyint               DEFAULT '0' COMMENT '是否查询字段',
    `query_type`     varchar(20)           DEFAULT 'EQ' COMMENT '查询方式：EQ/LIKE/BETWEEN/GT/LT等',
    `html_type`      varchar(20)           DEFAULT 'INPUT' COMMENT '显示类型：INPUT/SELECT/DATETIME/TEXTAREA等',
    `dict_type`      varchar(100)          DEFAULT NULL COMMENT '字典类型',
    `sort`           int                   DEFAULT '0' COMMENT '排序',
    `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`column_id`),
    KEY              `idx_table_id` (`table_id`)
) ENGINE=InnoDB AUTO_INCREMENT=60 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成表字段配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `gen_template`
--

DROP TABLE IF EXISTS `gen_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gen_template`
(
    `template_id`      bigint       NOT NULL AUTO_INCREMENT COMMENT '模板ID',
    `template_name`    varchar(100) NOT NULL COMMENT '模板名称',
    `template_code`    varchar(50)  NOT NULL COMMENT '模板编码',
    `template_type`    varchar(20)  NOT NULL COMMENT '模板类型：ENTITY/MAPPER/SERVICE/CONTROLLER/DTO/VO/SQL',
    `template_engine`  varchar(20)           DEFAULT 'VELOCITY' COMMENT '模板引擎',
    `template_content` longtext     NOT NULL COMMENT '模板内容',
    `file_suffix`      varchar(20)           DEFAULT '.java' COMMENT '生成文件后缀',
    `file_path`        varchar(200)          DEFAULT NULL COMMENT '生成文件路径（相对路径）',
    `is_system`        tinyint               DEFAULT '1' COMMENT '是否系统内置',
    `is_enabled`       tinyint               DEFAULT '1' COMMENT '是否启用',
    `sort`             int                   DEFAULT '0' COMMENT '排序',
    `remark`           varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `tenant_id`        bigint unsigned DEFAULT NULL,
    PRIMARY KEY (`template_id`),
    UNIQUE KEY `uk_template_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成模板配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `legal_case`
--

DROP TABLE IF EXISTS `legal_case`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `legal_case`
(
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '案件ID',
    `case_name`   varchar(100)      DEFAULT NULL COMMENT '案件名称',
    `case_status` varchar(10)       DEFAULT NULL COMMENT '案件状态(1:待办,0:已办)',
    `create_by`   bigint            DEFAULT NULL COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   bigint            DEFAULT NULL COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='案件信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qrtz_blob_triggers`
--

DROP TABLE IF EXISTS `qrtz_blob_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_blob_triggers`
(
    `sched_name`    varchar(120) NOT NULL COMMENT '调度名称',
    `trigger_name`  varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
    `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
    `blob_data`     blob COMMENT '存放持久化Trigger对象',
    PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`),
    CONSTRAINT `qrtz_blob_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='Blob类型的触发器表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qrtz_calendars`
--

DROP TABLE IF EXISTS `qrtz_calendars`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_calendars`
(
    `sched_name`    varchar(120) NOT NULL COMMENT '调度名称',
    `calendar_name` varchar(200) NOT NULL COMMENT '日历名称',
    `calendar`      blob         NOT NULL COMMENT '存放持久化calendar对象',
    PRIMARY KEY (`sched_name`, `calendar_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='日历信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qrtz_cron_triggers`
--

DROP TABLE IF EXISTS `qrtz_cron_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_cron_triggers`
(
    `sched_name`      varchar(120) NOT NULL COMMENT '调度名称',
    `trigger_name`    varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
    `trigger_group`   varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
    `cron_expression` varchar(200) NOT NULL COMMENT 'cron表达式',
    `time_zone_id`    varchar(80) DEFAULT NULL COMMENT '时区',
    PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`),
    CONSTRAINT `qrtz_cron_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='Cron类型的触发器表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qrtz_fired_triggers`
--

DROP TABLE IF EXISTS `qrtz_fired_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_fired_triggers`
(
    `sched_name`        varchar(120) NOT NULL COMMENT '调度名称',
    `entry_id`          varchar(95)  NOT NULL COMMENT '调度器实例id',
    `trigger_name`      varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
    `trigger_group`     varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
    `instance_name`     varchar(200) NOT NULL COMMENT '调度器实例名',
    `fired_time`        bigint       NOT NULL COMMENT '触发的时间',
    `sched_time`        bigint       NOT NULL COMMENT '定时器制定的时间',
    `priority`          int          NOT NULL COMMENT '优先级',
    `state`             varchar(16)  NOT NULL COMMENT '状态',
    `job_name`          varchar(200) DEFAULT NULL COMMENT '任务名称',
    `job_group`         varchar(200) DEFAULT NULL COMMENT '任务组名',
    `is_nonconcurrent`  varchar(1)   DEFAULT NULL COMMENT '是否并发',
    `requests_recovery` varchar(1)   DEFAULT NULL COMMENT '是否接受恢复执行',
    PRIMARY KEY (`sched_name`, `entry_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='已触发的触发器表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qrtz_job_details`
--

DROP TABLE IF EXISTS `qrtz_job_details`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_job_details`
(
    `sched_name`        varchar(120) NOT NULL COMMENT '调度名称',
    `job_name`          varchar(200) NOT NULL COMMENT '任务名称',
    `job_group`         varchar(200) NOT NULL COMMENT '任务组名',
    `description`       varchar(250) DEFAULT NULL COMMENT '相关介绍',
    `job_class_name`    varchar(250) NOT NULL COMMENT '执行任务类名称',
    `is_durable`        varchar(1)   NOT NULL COMMENT '是否持久化',
    `is_nonconcurrent`  varchar(1)   NOT NULL COMMENT '是否并发',
    `is_update_data`    varchar(1)   NOT NULL COMMENT '是否更新数据',
    `requests_recovery` varchar(1)   NOT NULL COMMENT '是否接受恢复执行',
    `job_data`          blob COMMENT '存放持久化job对象',
    PRIMARY KEY (`sched_name`, `job_name`, `job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='任务详细信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qrtz_locks`
--

DROP TABLE IF EXISTS `qrtz_locks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_locks`
(
    `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
    `lock_name`  varchar(40)  NOT NULL COMMENT '悲观锁名称',
    PRIMARY KEY (`sched_name`, `lock_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='存储的悲观锁信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qrtz_paused_trigger_grps`
--

DROP TABLE IF EXISTS `qrtz_paused_trigger_grps`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_paused_trigger_grps`
(
    `sched_name`    varchar(120) NOT NULL COMMENT '调度名称',
    `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
    PRIMARY KEY (`sched_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='暂停的触发器表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qrtz_scheduler_state`
--

DROP TABLE IF EXISTS `qrtz_scheduler_state`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_scheduler_state`
(
    `sched_name`        varchar(120) NOT NULL COMMENT '调度名称',
    `instance_name`     varchar(200) NOT NULL COMMENT '实例名称',
    `last_checkin_time` bigint       NOT NULL COMMENT '上次检查时间',
    `checkin_interval`  bigint       NOT NULL COMMENT '检查间隔时间',
    PRIMARY KEY (`sched_name`, `instance_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='调度器状态表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qrtz_simple_triggers`
--

DROP TABLE IF EXISTS `qrtz_simple_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_simple_triggers`
(
    `sched_name`      varchar(120) NOT NULL COMMENT '调度名称',
    `trigger_name`    varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
    `trigger_group`   varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
    `repeat_count`    bigint       NOT NULL COMMENT '重复的次数统计',
    `repeat_interval` bigint       NOT NULL COMMENT '重复的间隔时间',
    `times_triggered` bigint       NOT NULL COMMENT '已经触发的次数',
    PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`),
    CONSTRAINT `qrtz_simple_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='简单触发器的信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qrtz_simprop_triggers`
--

DROP TABLE IF EXISTS `qrtz_simprop_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_simprop_triggers`
(
    `sched_name`    varchar(120) NOT NULL COMMENT '调度名称',
    `trigger_name`  varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
    `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
    `str_prop_1`    varchar(512)   DEFAULT NULL COMMENT 'String类型的trigger的第一个参数',
    `str_prop_2`    varchar(512)   DEFAULT NULL COMMENT 'String类型的trigger的第二个参数',
    `str_prop_3`    varchar(512)   DEFAULT NULL COMMENT 'String类型的trigger的第三个参数',
    `int_prop_1`    int            DEFAULT NULL COMMENT 'int类型的trigger的第一个参数',
    `int_prop_2`    int            DEFAULT NULL COMMENT 'int类型的trigger的第二个参数',
    `long_prop_1`   bigint         DEFAULT NULL COMMENT 'long类型的trigger的第一个参数',
    `long_prop_2`   bigint         DEFAULT NULL COMMENT 'long类型的trigger的第二个参数',
    `dec_prop_1`    decimal(13, 4) DEFAULT NULL COMMENT 'decimal类型的trigger的第一个参数',
    `dec_prop_2`    decimal(13, 4) DEFAULT NULL COMMENT 'decimal类型的trigger的第二个参数',
    `bool_prop_1`   varchar(1)     DEFAULT NULL COMMENT 'Boolean类型的trigger的第一个参数',
    `bool_prop_2`   varchar(1)     DEFAULT NULL COMMENT 'Boolean类型的trigger的第二个参数',
    PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`),
    CONSTRAINT `qrtz_simprop_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='同步机制的行锁表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qrtz_triggers`
--

DROP TABLE IF EXISTS `qrtz_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_triggers`
(
    `sched_name`     varchar(120) NOT NULL COMMENT '调度名称',
    `trigger_name`   varchar(200) NOT NULL COMMENT '触发器的名字',
    `trigger_group`  varchar(200) NOT NULL COMMENT '触发器所属组的名字',
    `job_name`       varchar(200) NOT NULL COMMENT 'qrtz_job_details表job_name的外键',
    `job_group`      varchar(200) NOT NULL COMMENT 'qrtz_job_details表job_group的外键',
    `description`    varchar(250) DEFAULT NULL COMMENT '相关介绍',
    `next_fire_time` bigint       DEFAULT NULL COMMENT '上一次触发时间（毫秒）',
    `prev_fire_time` bigint       DEFAULT NULL COMMENT '下一次触发时间（默认为-1表示不触发）',
    `priority`       int          DEFAULT NULL COMMENT '优先级',
    `trigger_state`  varchar(16)  NOT NULL COMMENT '触发器状态',
    `trigger_type`   varchar(8)   NOT NULL COMMENT '触发器的类型',
    `start_time`     bigint       NOT NULL COMMENT '开始时间',
    `end_time`       bigint       DEFAULT NULL COMMENT '结束时间',
    `calendar_name`  varchar(200) DEFAULT NULL COMMENT '日程表名称',
    `misfire_instr`  smallint     DEFAULT NULL COMMENT '补偿执行的策略',
    `job_data`       blob COMMENT '存放持久化job对象',
    PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`),
    KEY              `sched_name` (`sched_name`,`job_name`,`job_group`),
    CONSTRAINT `qrtz_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `job_name`, `job_group`) REFERENCES `qrtz_job_details` (`sched_name`, `job_name`, `job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='触发器详细信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_api_config`
--

DROP TABLE IF EXISTS `sys_api_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_api_config`
(
    `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `api_name`         varchar(100) NOT NULL COMMENT '接口名称 (如:查询用户信息)',
    `api_code`         varchar(100) DEFAULT NULL COMMENT '接口编码 (用于程序逻辑引用)',
    `req_method`       varchar(20)  NOT NULL COMMENT '请求方式 (GET, POST, PUT, DELETE, ALL)',
    `url_path`         varchar(255) NOT NULL COMMENT '接口请求路径 (支持Ant风格, 如 /api/user/**)',
    `api_version`      varchar(20)  DEFAULT 'v1.0.0' COMMENT '接口版本号',
    `module_code`      varchar(50)  NOT NULL COMMENT '所属业务模块 (如: sys, order, pay)',
    `service_id`       varchar(100) DEFAULT NULL COMMENT '所属微服务ID (若为微服务架构)',
    `auth_flag`        tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否需认证/鉴权 (Token校验)',
    `encrypt_flag`     tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否需报文加解密 (敏感数据保护)',
    `tenant_flag`      tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用租户隔离 (数据权限)',
    `limit_flag`       tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否开启限流',
    `sensitive_fields` text COMMENT '需脱敏字段 (JSON数组存储, 如 ["phone", "id_card"])',
    `status`           tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态 (1-正常, 0-停用)',
    `remark`           varchar(500) DEFAULT NULL COMMENT '备注说明',
    `create_by`        varchar(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`      datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`        varchar(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`      datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `tenant_id`        bigint       DEFAULT NULL COMMENT '租户ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_method_url` (`url_path`,`req_method`) USING BTREE COMMENT '路径与方法组合唯一索引',
    KEY                `idx_module` (`module_code`) USING BTREE COMMENT '模块查询索引',
    KEY                `idx_tenant_id` (`tenant_id`) USING BTREE COMMENT '租户索引'
) ENGINE=InnoDB AUTO_INCREMENT=963 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='REST接口配置管理表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_auth_online_user`
--

DROP TABLE IF EXISTS `sys_auth_online_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_auth_online_user`
(
    `id`                 bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `token_value`        varchar(255) NOT NULL COMMENT 'Token值',
    `user_id`            bigint       NOT NULL COMMENT '用户ID',
    `username`           varchar(100) NOT NULL COMMENT '用户名',
    `real_name`          varchar(100) DEFAULT NULL COMMENT '真实姓名',
    `dept_name`          varchar(100) DEFAULT NULL COMMENT '部门名称',
    `ip_address`         varchar(50)  DEFAULT NULL COMMENT '登录IP地址',
    `login_location`     varchar(100) DEFAULT NULL COMMENT '登录地点',
    `browser`            varchar(50)  DEFAULT NULL COMMENT '浏览器类型',
    `os`                 varchar(50)  DEFAULT NULL COMMENT '操作系统',
    `login_time`         datetime     NOT NULL COMMENT '登录时间',
    `last_activity_time` datetime     DEFAULT NULL COMMENT '最后活动时间',
    `expire_time`        datetime     DEFAULT NULL COMMENT 'Token过期时间',
    `status`             tinyint      DEFAULT '1' COMMENT '状态: 1-在线, 0-已下线',
    `logout_time`        datetime     DEFAULT NULL COMMENT '登出时间',
    `logout_type`        tinyint      DEFAULT NULL COMMENT '登出类型: 1-主动登出, 2-被踢下线, 3-被顶下线, 4-Token过期',
    `tenant_id`          bigint       DEFAULT NULL COMMENT '租户ID',
    `create_time`        datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_token` (`token_value`),
    KEY                  `idx_user_id` (`user_id`),
    KEY                  `idx_status` (`status`),
    KEY                  `idx_login_time` (`login_time`),
    KEY                  `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=49 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='在线用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_auth_online_user_history`
--

DROP TABLE IF EXISTS `sys_auth_online_user_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_auth_online_user_history`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`         bigint       NOT NULL COMMENT '用户ID',
    `username`        varchar(100) NOT NULL COMMENT '用户名',
    `ip_address`      varchar(50)  DEFAULT NULL COMMENT '登录IP地址',
    `login_location`  varchar(100) DEFAULT NULL COMMENT '登录地点',
    `browser`         varchar(50)  DEFAULT NULL COMMENT '浏览器类型',
    `os`              varchar(50)  DEFAULT NULL COMMENT '操作系统',
    `login_time`      datetime     NOT NULL COMMENT '登录时间',
    `logout_time`     datetime     DEFAULT NULL COMMENT '登出时间',
    `online_duration` int          DEFAULT NULL COMMENT '在线时长(秒)',
    `logout_type`     tinyint      DEFAULT NULL COMMENT '登出类型: 1-主动登出, 2-被踢下线, 3-被顶下线, 4-Token过期',
    `tenant_id`       bigint       DEFAULT NULL COMMENT '租户ID',
    `create_time`     datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY               `idx_user_id` (`user_id`),
    KEY               `idx_login_time` (`login_time`),
    KEY               `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='在线用户历史表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_config`
--

DROP TABLE IF EXISTS `sys_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_config`
(
    `config_id`    bigint   NOT NULL AUTO_INCREMENT COMMENT '参数主键',
    `tenant_id`    bigint            DEFAULT '0' COMMENT '租户编号',
    `config_name`  varchar(100)      DEFAULT '' COMMENT '参数名称',
    `config_key`   varchar(100)      DEFAULT '' COMMENT '参数键名',
    `config_value` varchar(500)      DEFAULT '' COMMENT '参数键值',
    `config_type`  char(1)           DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
    `config_desc`  varchar(500)      DEFAULT NULL COMMENT '参数描述',
    `sort`         int               DEFAULT '0' COMMENT '排序',
    `create_by`    bigint            DEFAULT NULL COMMENT '创建者',
    `create_time`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`    bigint            DEFAULT NULL COMMENT '更新者',
    `update_time`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_dept`  bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    PRIMARY KEY (`config_id`),
    UNIQUE KEY `uk_tenant_config_key` (`tenant_id`,`config_key`),
    KEY            `idx_config_type` (`config_type`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_config_group`
--

DROP TABLE IF EXISTS `sys_config_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_config_group`
(
    `id`           bigint                                                        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `group_code`   varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  NOT NULL COMMENT '分组编码',
    `group_name`   varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分组名称',
    `group_icon`   varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL COMMENT '分组图标',
    `config_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '配置值(JSON格式)',
    `sort`         int                                                           DEFAULT '0' COMMENT '排序',
    `status`       tinyint                                                       DEFAULT '1' COMMENT '状态(0-禁用 1-启用)',
    `remark`       varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
    `create_time`  datetime                                                      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime                                                      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_group_code` (`group_code`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统配置分组表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_data_scope_config`
--

DROP TABLE IF EXISTS `sys_data_scope_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_data_scope_config`
(
    `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`        bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    `resource_code`    varchar(100) NOT NULL COMMENT '资源编码（对应接口路径或功能模块）',
    `resource_name`    varchar(100)          DEFAULT NULL COMMENT '资源名称',
    `mapper_method`    varchar(200) NOT NULL COMMENT 'Mapper方法（如：com.example.mapper.UserMapper.selectList）',
    `table_alias`      varchar(50)           DEFAULT 't' COMMENT '主表别名',
    `user_id_column`   text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '用户ID字段名',
    `org_id_column`    text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '组织ID字段名',
    `tenant_id_column` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '租户ID字段名',
    `enabled`          tinyint               DEFAULT '1' COMMENT '是否启用（0-禁用，1-启用）',
    `remark`           varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_by`        bigint                DEFAULT NULL COMMENT '创建者',
    `create_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`        bigint                DEFAULT NULL COMMENT '更新者',
    `update_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_dept`      bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_mapper` (`tenant_id`,`mapper_method`),
    KEY                `idx_resource_code` (`resource_code`),
    KEY                `idx_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据权限配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_dict_data`
--

DROP TABLE IF EXISTS `sys_dict_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_data`
(
    `dict_code`         bigint   NOT NULL AUTO_INCREMENT COMMENT '字典编码',
    `tenant_id`         bigint            DEFAULT '0' COMMENT '租户编号',
    `dict_sort`         int               DEFAULT '0' COMMENT '字典排序',
    `dict_label`        varchar(100)      DEFAULT '' COMMENT '字典标签',
    `dict_value`        varchar(100)      DEFAULT '' COMMENT '字典键值',
    `dict_type`         varchar(100)      DEFAULT '' COMMENT '字典类型',
    `css_class`         varchar(100)      DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
    `list_class`        varchar(100)      DEFAULT NULL COMMENT '表格回显样式',
    `is_default`        char(1)           DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
    `parent_dict_code`  bigint unsigned DEFAULT NULL COMMENT '上级字典编码',
    `linked_dict_type`  varchar(100)      DEFAULT NULL COMMENT '关联的字典类型',
    `linked_dict_value` varchar(100)      DEFAULT NULL COMMENT '关联的字典值',
    `dict_status`       tinyint           DEFAULT '1' COMMENT '字典状态（0-禁用，1-正常）',
    `remark`            varchar(500)      DEFAULT NULL COMMENT '备注',
    `create_by`         bigint            DEFAULT NULL COMMENT '创建者',
    `create_time`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`         bigint            DEFAULT NULL COMMENT '更新者',
    `update_time`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_dept`       bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    PRIMARY KEY (`dict_code`),
    UNIQUE KEY `uk_tenant_dict_data` (`tenant_id`,`dict_type`,`dict_value`),
    KEY                 `idx_tenant_dict_type` (`tenant_id`,`dict_type`),
    KEY                 `idx_parent_dict_code` (`parent_dict_code`),
    KEY                 `idx_linked_dict` (`linked_dict_type`,`linked_dict_value`)
) ENGINE=InnoDB AUTO_INCREMENT=45 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_dict_type`
--

DROP TABLE IF EXISTS `sys_dict_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_type`
(
    `dict_id`     bigint   NOT NULL AUTO_INCREMENT COMMENT '字典主键',
    `tenant_id`   bigint            DEFAULT '0' COMMENT '租户编号',
    `dict_name`   varchar(100)      DEFAULT '' COMMENT '字典名称',
    `dict_type`   varchar(100)      DEFAULT '' COMMENT '字典类型（租户内唯一）',
    `dict_status` tinyint           DEFAULT '1' COMMENT '字典状态（0-禁用，1-正常）',
    `remark`      varchar(500)      DEFAULT NULL COMMENT '备注',
    `create_by`   bigint            DEFAULT NULL COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   bigint            DEFAULT NULL COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    PRIMARY KEY (`dict_id`),
    UNIQUE KEY `uk_tenant_dict_type` (`tenant_id`,`dict_type`),
    KEY           `idx_dict_status` (`dict_status`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典类型表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_excel_column_config`
--

DROP TABLE IF EXISTS `sys_excel_column_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_excel_column_config`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_key`    varchar(100) NOT NULL COMMENT '关联配置键',
    `field_name`    varchar(100) NOT NULL COMMENT '字段名（实体属性名）',
    `column_name`   varchar(200) NOT NULL COMMENT '列名（Excel表头）',
    `width`         int         DEFAULT '20' COMMENT '列宽',
    `order_num`     int         DEFAULT '0' COMMENT '排序（值越小越靠前）',
    `export`        tinyint(1) DEFAULT '1' COMMENT '是否导出（1-是，0-否）',
    `date_format`   varchar(50) DEFAULT NULL COMMENT '日期格式（如：yyyy-MM-dd）',
    `number_format` varchar(50) DEFAULT NULL COMMENT '数字格式',
    `dict_type`     varchar(50) DEFAULT NULL COMMENT '字典类型（关联sys_dict_type）',
    `create_time`   datetime    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     bigint      DEFAULT NULL COMMENT '创建者',
    `update_by`     bigint      DEFAULT NULL COMMENT '更新者',
    `create_dept`   bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    PRIMARY KEY (`id`),
    KEY             `idx_config_key` (`config_key`),
    KEY             `idx_order` (`order_num`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Excel列配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_excel_export_config`
--

DROP TABLE IF EXISTS `sys_excel_export_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_excel_export_config`
(
    `id`                 bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_key`         varchar(100) NOT NULL COMMENT '配置键（唯一标识）',
    `export_name`        varchar(200) NOT NULL COMMENT '导出名称',
    `sheet_name`         varchar(100) DEFAULT 'Sheet1' COMMENT 'Sheet名称',
    `file_name_template` varchar(200) DEFAULT NULL COMMENT '文件名模板（支持占位符：{date}、{time}）',
    `data_source_bean`   varchar(100) NOT NULL COMMENT '数据源Bean名称（如：userService）',
    `query_method`       varchar(100) NOT NULL COMMENT '数据查询方法名（如：list、page）',
    `auto_trans`         tinyint(1) DEFAULT '1' COMMENT '是否自动翻译字典（1-是，0-否）',
    `pageable`           tinyint(1) DEFAULT '0' COMMENT '是否分页查询（1-是，0-否）',
    `max_rows`           int          DEFAULT '10000' COMMENT '最大导出条数',
    `sort_field`         varchar(50)  DEFAULT NULL COMMENT '排序字段',
    `sort_order`         varchar(10)  DEFAULT NULL COMMENT '排序方向（ASC/DESC）',
    `status`             tinyint(1) DEFAULT '1' COMMENT '状态（1-启用，0-禁用）',
    `remark`             varchar(500) DEFAULT NULL COMMENT '备注',
    `create_time`        datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`          bigint       DEFAULT NULL COMMENT '创建者',
    `update_by`          bigint       DEFAULT NULL COMMENT '更新者',
    `create_dept`        bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `config_key` (`config_key`),
    KEY                  `idx_config_key` (`config_key`)
) ENGINE=InnoDB AUTO_INCREMENT=2013984223186178051 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Excel导出配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_file_group`
--

DROP TABLE IF EXISTS `sys_file_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_file_group`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `group_name`  varchar(100) NOT NULL COMMENT '分组名称',
    `group_code`  varchar(100) DEFAULT NULL COMMENT '分组编码',
    `group_type`  varchar(50)  DEFAULT 'default' COMMENT '分组类型(document-文档,image-图片,video-视频,audio-音频,archive-压缩包,default-默认)',
    `parent_id`   bigint       DEFAULT NULL COMMENT '父分组ID',
    `sort`        int          DEFAULT '0' COMMENT '排序',
    `icon`        varchar(100) DEFAULT NULL COMMENT '图标',
    `description` varchar(500) DEFAULT NULL COMMENT '描述',
    `status`      tinyint(1) DEFAULT '1' COMMENT '状态(1-正常,0-禁用)',
    `create_time` datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   bigint       DEFAULT NULL COMMENT '创建者',
    `update_by`   bigint       DEFAULT NULL COMMENT '更新者',
    `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    `deleted`     tinyint(1) DEFAULT '0' COMMENT '删除标记(0-未删除,1-已删除)',
    PRIMARY KEY (`id`),
    KEY           `idx_parent_id` (`parent_id`),
    KEY           `idx_group_type` (`group_type`),
    KEY           `idx_group_code` (`group_code`)
) ENGINE=InnoDB AUTO_INCREMENT=2028326543566475267 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件分组表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_file_metadata`
--

DROP TABLE IF EXISTS `sys_file_metadata`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_file_metadata`
(
    `id`             bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `file_id`        varchar(64)   NOT NULL COMMENT '文件唯一ID',
    `group_id`       bigint        DEFAULT NULL COMMENT '分组ID',
    `original_name`  varchar(500)  NOT NULL COMMENT '原始文件名',
    `storage_name`   varchar(500)  NOT NULL COMMENT '存储文件名',
    `file_path`      varchar(1000) NOT NULL COMMENT '文件路径',
    `file_size`      bigint        NOT NULL COMMENT '文件大小(字节)',
    `mime_type`      varchar(200)  DEFAULT NULL COMMENT '文件MIME类型',
    `extension`      varchar(50)   DEFAULT NULL COMMENT '文件扩展名',
    `md5`            varchar(64)   DEFAULT NULL COMMENT '文件MD5',
    `storage_type`   varchar(50)   NOT NULL COMMENT '存储策略',
    `bucket`         varchar(100)  DEFAULT NULL COMMENT '存储桶/命名空间',
    `access_url`     varchar(1000) DEFAULT NULL COMMENT '访问URL',
    `thumbnail_url`  varchar(1000) DEFAULT NULL COMMENT '缩略图URL',
    `business_type`  varchar(100)  DEFAULT NULL COMMENT '业务类型',
    `business_id`    varchar(100)  DEFAULT NULL COMMENT '业务ID',
    `uploader_id`    bigint        DEFAULT NULL COMMENT '上传者ID',
    `upload_time`    datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `expire_time`    datetime      DEFAULT NULL COMMENT '过期时间',
    `is_private`     tinyint(1) DEFAULT '0' COMMENT '是否私有',
    `download_count` int           DEFAULT '0' COMMENT '下载次数',
    `status`         tinyint(1) DEFAULT '1' COMMENT '状态(1-正常,0-已删除)',
    `create_time`    datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      bigint        DEFAULT NULL COMMENT '创建者',
    `update_by`      bigint        DEFAULT NULL COMMENT '更新者',
    `create_dept`    bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `file_id` (`file_id`),
    KEY              `idx_file_id` (`file_id`),
    KEY              `idx_md5` (`md5`),
    KEY              `idx_business` (`business_type`,`business_id`),
    KEY              `idx_uploader` (`uploader_id`),
    KEY              `idx_upload_time` (`upload_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2038076910762115075 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件元数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_file_storage_config`
--

DROP TABLE IF EXISTS `sys_file_storage_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_file_storage_config`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_name`   varchar(100) NOT NULL COMMENT '配置名称',
    `storage_type`  varchar(50)  NOT NULL COMMENT '存储类型(local/minio/aliyun_oss等)',
    `is_default`    tinyint(1) DEFAULT '0' COMMENT '是否默认策略',
    `enabled`       tinyint(1) DEFAULT '1' COMMENT '是否启用',
    `endpoint`      varchar(500) DEFAULT NULL COMMENT '访问端点',
    `access_key`    varchar(200) DEFAULT NULL COMMENT '访问密钥ID',
    `secret_key`    varchar(200) DEFAULT NULL COMMENT '访问密钥Secret',
    `bucket_name`   varchar(100) DEFAULT NULL COMMENT '存储桶名称',
    `region`        varchar(100) DEFAULT NULL COMMENT '区域',
    `base_path`     varchar(200) DEFAULT NULL COMMENT '基础路径',
    `domain`        varchar(500) DEFAULT NULL COMMENT '访问域名',
    `use_https`     tinyint(1) DEFAULT '1' COMMENT '是否使用HTTPS',
    `max_file_size` int          DEFAULT '100' COMMENT '最大文件大小(MB)',
    `allowed_types` varchar(500) DEFAULT NULL COMMENT '允许的文件类型(逗号分隔)',
    `order_num`     int          DEFAULT '0' COMMENT '排序',
    `extra_config`  text COMMENT '扩展配置(JSON)',
    `create_time`   datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     bigint       DEFAULT NULL COMMENT '创建者',
    `update_by`     bigint       DEFAULT NULL COMMENT '更新者',
    `create_dept`   bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    PRIMARY KEY (`id`),
    KEY             `idx_storage_type` (`storage_type`),
    KEY             `idx_is_default` (`is_default`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件存储配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_approval_level`
--

DROP TABLE IF EXISTS `sys_flow_approval_level`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_flow_approval_level`
(
    `id`             varchar(64) NOT NULL COMMENT '主键',
    `node_config_id` varchar(64) NOT NULL COMMENT '节点配置ID',
    `level_index`    int         NOT NULL COMMENT '层级序号',
    `level_name`     varchar(100) DEFAULT NULL COMMENT '层级名称',
    `assignee_type`  varchar(50)  DEFAULT NULL COMMENT '审批人类型',
    `assignee_value` text COMMENT '审批人值',
    `condition_expr` varchar(500) DEFAULT NULL COMMENT '层级条件表达式',
    `skip_condition` varchar(500) DEFAULT NULL COMMENT '跳过条件',
    `sort_order`     int          DEFAULT '0' COMMENT '排序',
    `create_time`    datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY              `idx_node_config_id` (`node_config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批层级配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_business`
--

DROP TABLE IF EXISTS `sys_flow_business`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_flow_business`
(
    `id`                  varchar(64)  NOT NULL COMMENT '主键',
    `business_key`        varchar(100) NOT NULL COMMENT '业务Key',
    `business_type`       varchar(50)  NOT NULL COMMENT '业务类型（order/contract等）',
    `process_instance_id` varchar(64)  DEFAULT NULL COMMENT '流程实例ID',
    `process_def_id`      varchar(64)  DEFAULT NULL COMMENT '流程定义ID',
    `process_def_key`     varchar(100) DEFAULT NULL COMMENT '流程定义KEY',
    `title`               varchar(200) DEFAULT NULL COMMENT '流程标题',
    `status`              varchar(50)  DEFAULT 'draft' COMMENT '业务状态（draft-草稿/running-审批中/approved-已通过/rejected-已驳回/canceled-已取消）',
    `apply_user_id`       varchar(64)  DEFAULT NULL COMMENT '申请人',
    `apply_user_name`     varchar(100) DEFAULT NULL COMMENT '申请人姓名',
    `apply_dept_id`       varchar(64)  DEFAULT NULL COMMENT '申请部门ID',
    `apply_dept_name`     varchar(100) DEFAULT NULL COMMENT '申请部门名称',
    `apply_time`          datetime     DEFAULT NULL COMMENT '申请时间',
    `end_time`            datetime     DEFAULT NULL COMMENT '结束时间',
    `duration`            bigint       DEFAULT NULL COMMENT '流程耗时（毫秒）',
    `create_time`         datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_business_key` (`business_key`),
    KEY                   `idx_process_instance_id` (`process_instance_id`),
    KEY                   `idx_apply_user_id` (`apply_user_id`),
    KEY                   `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程业务关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_category`
--

DROP TABLE IF EXISTS `sys_flow_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_flow_category`
(
    `id`            varchar(64)  NOT NULL COMMENT '主键',
    `category_code` varchar(50)  NOT NULL COMMENT '分类编码',
    `category_name` varchar(100) NOT NULL COMMENT '分类名称',
    `description`   varchar(500) DEFAULT NULL COMMENT '描述',
    `sort_order`    int          DEFAULT '0' COMMENT '排序',
    `status`        tinyint      DEFAULT '1' COMMENT '状态（0-禁用/1-启用）',
    `create_time`   datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `category_code` (`category_code`),
    KEY             `idx_category_code` (`category_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程分类表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_cc`
--

DROP TABLE IF EXISTS `sys_flow_cc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_flow_cc`
(
    `id`                  varchar(64) NOT NULL COMMENT '主键',
    `process_instance_id` varchar(64)  DEFAULT NULL COMMENT '流程实例ID',
    `process_def_key`     varchar(100) DEFAULT NULL COMMENT '流程定义KEY',
    `task_id`             varchar(64)  DEFAULT NULL COMMENT '来源任务ID',
    `title`               varchar(200) DEFAULT NULL COMMENT '标题',
    `content`             text COMMENT '内容摘要',
    `business_key`        varchar(100) DEFAULT NULL COMMENT '业务Key',
    `cc_user_id`          varchar(64) NOT NULL COMMENT '抄送人ID',
    `cc_user_name`        varchar(100) DEFAULT NULL COMMENT '抄送人姓名',
    `send_user_id`        varchar(64)  DEFAULT NULL COMMENT '发送人ID',
    `send_user_name`      varchar(100) DEFAULT NULL COMMENT '发送人姓名',
    `cc_time`             datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '抄送时间',
    `read_time`           datetime     DEFAULT NULL COMMENT '阅读时间',
    `is_read`             tinyint      DEFAULT '0' COMMENT '是否已读（0-未读/1-已读）',
    PRIMARY KEY (`id`),
    KEY                   `idx_process_instance_id` (`process_instance_id`),
    KEY                   `idx_cc_user_id` (`cc_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程抄送表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_comment`
--

DROP TABLE IF EXISTS `sys_flow_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_flow_comment`
(
    `id`                  varchar(64) NOT NULL COMMENT '主键',
    `process_instance_id` varchar(64)  DEFAULT NULL COMMENT '流程实例ID',
    `process_def_key`     varchar(100) DEFAULT NULL COMMENT '流程定义KEY',
    `task_id`             varchar(64)  DEFAULT NULL COMMENT '任务ID',
    `task_name`           varchar(200) DEFAULT NULL COMMENT '任务名称',
    `type`                varchar(20)  DEFAULT 'comment' COMMENT '类型（comment-审批意见/event-流程事件）',
    `message`             text COMMENT '意见内容',
    `user_id`             varchar(64)  DEFAULT NULL COMMENT '用户ID',
    `user_name`           varchar(100) DEFAULT NULL COMMENT '用户姓名',
    `full_message`        text COMMENT '完整消息（JSON）',
    `create_time`         datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY                   `idx_process_instance_id` (`process_instance_id`),
    KEY                   `idx_task_id` (`task_id`),
    KEY                   `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程意见表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_condition_item`
--

DROP TABLE IF EXISTS `sys_flow_condition_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_flow_condition_item`
(
    `id`              varchar(64)  NOT NULL COMMENT '主键ID',
    `rule_id`         varchar(64)  NOT NULL COMMENT '规则ID',
    `field_name`      varchar(100) NOT NULL COMMENT '字段名称',
    `field_label`     varchar(100) DEFAULT NULL COMMENT '字段标签',
    `field_type`      varchar(32)  DEFAULT 'string' COMMENT '字段类型：string/number/date/boolean/user/dept/role',
    `operator`        varchar(32)  NOT NULL COMMENT '操作符：eq/ne/gt/lt/ge/le/contains/startsWith/endsWith/in/notIn/isEmpty/isNotEmpty',
    `value`           text COMMENT '比较值（JSON格式，支持多值）',
    `logic_connector` varchar(10)  DEFAULT 'and' COMMENT '逻辑连接符：and/or',
    `group_id`        varchar(64)  DEFAULT NULL COMMENT '分组ID（用于条件分组）',
    `sort_order`      int          DEFAULT '0' COMMENT '排序号',
    `create_time`     datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY               `idx_rule_id` (`rule_id`),
    KEY               `idx_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='条件规则项表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_condition_rule`
--

DROP TABLE IF EXISTS `sys_flow_condition_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_flow_condition_rule`
(
    `id`                   varchar(64)  NOT NULL COMMENT '主键ID',
    `rule_name`            varchar(100) NOT NULL COMMENT '规则名称',
    `rule_code`            varchar(100) DEFAULT NULL COMMENT '规则编码',
    `model_id`             varchar(64)  DEFAULT NULL COMMENT '模型ID',
    `sequence_flow_id`     varchar(128) NOT NULL COMMENT '序列流ID（BPMN中的SequenceFlow ID）',
    `condition_type`       varchar(32)  DEFAULT 'simple' COMMENT '条件类型：simple-简单条件，composite-组合条件，script-脚本',
    `condition_expression` text COMMENT '条件表达式（JSON格式存储）',
    `priority`             int          DEFAULT '0' COMMENT '优先级（数字越小优先级越高）',
    `is_default`           tinyint(1) DEFAULT '0' COMMENT '是否默认路径',
    `status`               int          DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
    `remark`               varchar(500) DEFAULT NULL COMMENT '备注',
    `create_by`            varchar(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`          datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`            varchar(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`          datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`              tinyint(1) DEFAULT '0' COMMENT '删除标志',
    PRIMARY KEY (`id`),
    KEY                    `idx_model_id` (`model_id`),
    KEY                    `idx_sequence_flow_id` (`sequence_flow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程条件规则表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_form`
--

DROP TABLE IF EXISTS `sys_flow_form`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_flow_form`
(
    `id`             bigint       NOT NULL COMMENT '主键ID',
    `form_key`       varchar(100) NOT NULL COMMENT '表单Key（唯一标识）',
    `form_name`      varchar(200) NOT NULL COMMENT '表单名称',
    `form_type`      varchar(50)  DEFAULT 'dynamic' COMMENT '表单类型（dynamic-动态表单/external-外部表单/builtin-内置表单）',
    `form_schema`    text COMMENT '表单Schema（JSON格式）',
    `form_url`       varchar(500) DEFAULT NULL COMMENT '外部表单URL（formType为external时使用）',
    `component_path` varchar(200) DEFAULT NULL COMMENT '内置表单组件路径（formType为builtin时使用）',
    `form_config`    text COMMENT '表单配置（JSON格式，包含校验规则、事件等）',
    `version`        int          DEFAULT '1' COMMENT '版本号',
    `status`         tinyint      DEFAULT '1' COMMENT '状态（0-禁用/1-启用）',
    `description`    varchar(500) DEFAULT NULL COMMENT '描述',
    `tenant_id`      bigint       DEFAULT NULL COMMENT '租户ID',
    `create_by`      varchar(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`    datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`      varchar(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`    datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        tinyint      DEFAULT '0' COMMENT '删除标志（0-正常/1-删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `form_key` (`form_key`),
    KEY              `idx_form_key` (`form_key`),
    KEY              `idx_form_name` (`form_name`),
    KEY              `idx_status` (`status`),
    KEY              `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程表单定义表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_model`
--

DROP TABLE IF EXISTS `sys_flow_model`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_flow_model`
(
    `id`                    varchar(64)  NOT NULL COMMENT '主键',
    `model_key`             varchar(100) NOT NULL COMMENT '模型标识',
    `model_name`            varchar(200) NOT NULL COMMENT '模型名称',
    `description`           varchar(500) DEFAULT NULL COMMENT '描述',
    `category`              varchar(100) DEFAULT NULL COMMENT '分类',
    `flow_type`             varchar(50)  DEFAULT NULL COMMENT '流程类型（leave-请假/expense-报销/approval-审批）',
    `form_type`             varchar(50)  DEFAULT 'dynamic' COMMENT '表单类型（dynamic-动态表单/custom-业务表单）',
    `form_id`               varchar(64)  DEFAULT NULL COMMENT '表单ID（业务表单时使用）',
    `form_json`             text COMMENT '动态表单JSON配置',
    `version`               int          DEFAULT '1' COMMENT '版本号',
    `process_definition_id` varchar(64)  DEFAULT NULL COMMENT 'Flowable流程定义ID',
    `deployment_id`         varchar(64)  DEFAULT NULL COMMENT 'Flowable部署ID',
    `deployment_key`        varchar(100) DEFAULT NULL COMMENT '部署KEY（发布后生成）',
    `status`                tinyint      DEFAULT '0' COMMENT '状态（0-设计/1-已发布/2-禁用）',
    `deploy_time`           datetime     DEFAULT NULL COMMENT '发布时间',
    `last_update_by`        varchar(64)  DEFAULT NULL COMMENT '最后修改人',
    `create_by`             varchar(64)  DEFAULT NULL COMMENT '创建人',
    `create_time`           datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`           datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`              tinyint      DEFAULT '0' COMMENT '删除标志（0-正常/1-删除）',
    `bpmn_xml`              longtext,
    `notify_type`           varchar(30)  DEFAULT NULL COMMENT '事件通知方式：none-不通知 / redis-Redis Pub/Sub / webhook-HTTP Webhook（互斥）',
    `webhook_url`           varchar(500) DEFAULT NULL COMMENT 'Webhook 回调地址，仅 notify_type=webhook 时生效',
    PRIMARY KEY (`id`),
    UNIQUE KEY `model_key` (`model_key`),
    KEY                     `idx_model_key` (`model_key`),
    KEY                     `idx_category` (`category`),
    KEY                     `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程模型表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_node_config`
--

DROP TABLE IF EXISTS `sys_flow_node_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_flow_node_config`
(
    `id`                    varchar(64)  NOT NULL COMMENT '主键',
    `model_id`              varchar(64)  NOT NULL COMMENT '流程模型ID',
    `node_id`               varchar(100) NOT NULL COMMENT '节点ID',
    `node_name`             varchar(200)                                                 DEFAULT NULL COMMENT '节点名称',
    `node_type`             varchar(50)                                                  DEFAULT 'approval' COMMENT '节点类型',
    `assignee_type`         varchar(50)                                                  DEFAULT NULL COMMENT '审批人类型(user/role/dept/post/expr)',
    `assignee_value`        text COMMENT '审批人值',
    `assignee_expr`         varchar(500)                                                 DEFAULT NULL COMMENT '审批人表达式',
    `multi_instance_type`   varchar(50)                                                  DEFAULT 'none' COMMENT '会签类型(none/sequential/parallel)',
    `completion_condition`  varchar(500)                                                 DEFAULT NULL COMMENT '完成条件',
    `pass_rate`             decimal(5, 2)                                                DEFAULT NULL COMMENT '通过比例',
    `due_date_days`         int                                                          DEFAULT NULL COMMENT '超时天数',
    `due_date_hours`        int                                                          DEFAULT NULL COMMENT '超时小时数',
    `timeout_action`        varchar(50)                                                  DEFAULT NULL COMMENT '超时动作(auto_pass/auto_reject/notify)',
    `allow_delegate`        tinyint                                                      DEFAULT '1' COMMENT '允许转办',
    `allow_transfer`        tinyint                                                      DEFAULT '1' COMMENT '允许转交',
    `allow_add_sign`        tinyint                                                      DEFAULT '0' COMMENT '允许加签',
    `allow_counter_sign`    tinyint                                                      DEFAULT '0' COMMENT '允许减签',
    `form_permission`       text COMMENT '表单字段权限配置JSON',
    `create_time`           datetime                                                     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`           datetime                                                     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `timeout_notify_users`  text,
    `allow_reject`          varchar(10)                                                  DEFAULT NULL,
    `allow_reject_to_start` varchar(10)                                                  DEFAULT NULL,
    `allow_withdraw`        varchar(10)                                                  DEFAULT NULL,
    `form_key`              varchar(30)                                                  DEFAULT NULL,
    `priority`              int                                                          DEFAULT NULL,
    `task_listeners`        text,
    `ext_config`            varchar(100)                                                 DEFAULT NULL,
    `status`                varchar(10)                                                  DEFAULT NULL,
    `last_update_by`        varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '最后修改人',
    `create_by`             varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建人',
    `del_flag`              tinyint                                                      DEFAULT '0' COMMENT '删除标志（0-正常/1-删除）',
    `update_by`             varchar(20)                                                  DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_node` (`model_id`,`node_id`),
    KEY                     `idx_model_id` (`model_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批节点配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_statistics`
--

DROP TABLE IF EXISTS `sys_flow_statistics`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_flow_statistics`
(
    `id`                   varchar(64)  NOT NULL COMMENT '主键',
    `process_def_key`      varchar(100) NOT NULL COMMENT '流程定义KEY',
    `stat_date`            date         NOT NULL COMMENT '统计日期',
    `total_instances`      int      DEFAULT '0' COMMENT '总实例数',
    `running_instances`    int      DEFAULT '0' COMMENT '运行中实例数',
    `completed_instances`  int      DEFAULT '0' COMMENT '已完成实例数',
    `terminated_instances` int      DEFAULT '0' COMMENT '已终止实例数',
    `avg_duration`         bigint   DEFAULT NULL COMMENT '平均耗时(毫秒)',
    `max_duration`         bigint   DEFAULT NULL COMMENT '最大耗时',
    `min_duration`         bigint   DEFAULT NULL COMMENT '最小耗时',
    `total_tasks`          int      DEFAULT '0' COMMENT '总任务数',
    `avg_task_duration`    bigint   DEFAULT NULL COMMENT '平均任务耗时',
    `timeout_tasks`        int      DEFAULT '0' COMMENT '超时任务数',
    `create_time`          datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_process_date` (`process_def_key`,`stat_date`),
    KEY                    `idx_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程监控统计表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_task`
--

DROP TABLE IF EXISTS `sys_flow_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_flow_task`
(
    `id`                  varchar(64)  NOT NULL COMMENT '主键',
    `task_id`             varchar(64)   DEFAULT NULL COMMENT 'Flowable任务ID',
    `task_name`           varchar(200)  DEFAULT NULL COMMENT '任务名称',
    `task_def_key`        varchar(100)  DEFAULT NULL COMMENT '任务定义Key',
    `task_def_id`         varchar(100)  DEFAULT NULL COMMENT '任务定义ID',
    `process_instance_id` varchar(64)   DEFAULT NULL COMMENT '流程实例ID',
    `process_def_id`      varchar(64)   DEFAULT NULL COMMENT '流程定义ID',
    `process_def_key`     varchar(100)  DEFAULT NULL COMMENT '流程定义KEY',
    `business_key`        varchar(100)  DEFAULT NULL COMMENT '业务Key',
    `business_type`       varchar(50)   DEFAULT NULL COMMENT '业务类型',
    `title`               varchar(200) NOT NULL COMMENT '任务标题',
    `assignee`            varchar(64)   DEFAULT NULL COMMENT '处理人（签收后）',
    `assignee_name`       varchar(100)  DEFAULT NULL COMMENT '处理人姓名',
    `candidate_users`     varchar(1000) DEFAULT NULL COMMENT '候选人（逗号分隔）',
    `candidate_groups`    varchar(1000) DEFAULT NULL COMMENT '候选组（逗号分隔）',
    `owner`               varchar(64)   DEFAULT NULL COMMENT '任务拥有人',
    `due_date`            datetime      DEFAULT NULL COMMENT '截止日期',
    `priority`            int           DEFAULT '50' COMMENT '优先级（0-100）',
    `status`              tinyint       DEFAULT '0' COMMENT '状态（0-待办/1-已签收/2-已通过/3-已驳回/4-已转办/5-已委派/6-已撤回）',
    `comment`             text COMMENT '审批意见',
    `attachment_urls`     text COMMENT '附件URL（逗号分隔）',
    `start_user_id`       varchar(64)   DEFAULT NULL COMMENT '流程发起人',
    `start_user_name`     varchar(100)  DEFAULT NULL COMMENT '发起人姓名',
    `start_dept_id`       varchar(64)   DEFAULT NULL COMMENT '发起部门ID',
    `start_dept_name`     varchar(100)  DEFAULT NULL COMMENT '发起部门名称',
    `create_time`         datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `claim_time`          datetime      DEFAULT NULL COMMENT '签收时间',
    `complete_time`       datetime      DEFAULT NULL COMMENT '完成时间',
    PRIMARY KEY (`id`),
    KEY                   `idx_task_id` (`task_id`),
    KEY                   `idx_process_instance_id` (`process_instance_id`),
    KEY                   `idx_assignee` (`assignee`),
    KEY                   `idx_status` (`status`),
    KEY                   `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程任务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_template`
--

DROP TABLE IF EXISTS `sys_flow_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_flow_template`
(
    `id`            varchar(64)  NOT NULL COMMENT '主键',
    `template_key`  varchar(100) NOT NULL COMMENT '模板标识',
    `template_name` varchar(200) NOT NULL COMMENT '模板名称',
    `category`      varchar(50)  DEFAULT NULL COMMENT '分类',
    `description`   varchar(500) DEFAULT NULL COMMENT '描述',
    `icon`          varchar(100) DEFAULT NULL COMMENT '图标',
    `form_type`     varchar(50)  DEFAULT NULL COMMENT '表单类型',
    `form_json`     text COMMENT '表单JSON',
    `bpmn_xml`      text COMMENT 'BPMN流程XML',
    `thumbnail`     varchar(500) DEFAULT NULL COMMENT '缩略图',
    `variables`     text COMMENT '流程变量定义（JSON）',
    `version`       int          DEFAULT '1' COMMENT '版本',
    `status`        tinyint      DEFAULT '1' COMMENT '状态（0-禁用/1-启用）',
    `usage_count`   int          DEFAULT '0' COMMENT '使用次数',
    `create_by`     varchar(64)  DEFAULT NULL COMMENT '创建人',
    `create_time`   datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_system`     varchar(10)  DEFAULT NULL,
    `sort_order`    int          DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `template_key` (`template_key`),
    KEY             `idx_template_key` (`template_key`),
    KEY             `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程模板表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_id_sequence`
--

DROP TABLE IF EXISTS `sys_id_sequence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_id_sequence`
(
    `biz_key`      varchar(100) NOT NULL COMMENT '业务维度唯一键',
    `max_id`       bigint       NOT NULL DEFAULT '0' COMMENT '当前已分配的最大ID',
    `step`         int          NOT NULL DEFAULT '1000' COMMENT '步长',
    `version`      int          NOT NULL DEFAULT '0' COMMENT '版本（乐观锁）',
    `reset_policy` varchar(20)           DEFAULT 'NONE' COMMENT '重置策略：NONE/DAILY/HOURLY',
    `seq_length`   int                   DEFAULT '8' COMMENT '序列长度（左侧补零）',
    `prefix`       varchar(50)           DEFAULT NULL COMMENT '前缀',
    PRIMARY KEY (`biz_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分布式ID序列配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_job_config`
--

DROP TABLE IF EXISTS `sys_job_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_job_config`
(
    `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `job_name`         varchar(200) NOT NULL COMMENT '任务名称',
    `job_group`        varchar(200) NOT NULL DEFAULT 'DEFAULT' COMMENT '任务分组',
    `description`      varchar(500)          DEFAULT NULL COMMENT '任务描述',
    `executor_bean`    varchar(200)          DEFAULT NULL COMMENT '执行器Bean名称',
    `executor_method`  varchar(200)          DEFAULT NULL COMMENT '执行器方法名',
    `executor_handler` varchar(200)          DEFAULT NULL COMMENT '执行器Handler',
    `executor_service` varchar(200)          DEFAULT NULL COMMENT '执行器服务名（RPC模式）',
    `cron_expression`  varchar(100) NOT NULL COMMENT 'Cron表达式',
    `job_param`        text COMMENT '任务参数',
    `status`           tinyint      NOT NULL DEFAULT '1' COMMENT '状态：0-停止 1-运行',
    `execute_mode`     varchar(20)  NOT NULL DEFAULT 'HANDLER' COMMENT '执行模式：BEAN/HANDLER',
    `retry_count`      int                   DEFAULT '0' COMMENT '失败重试次数',
    `alarm_email`      varchar(500)          DEFAULT NULL COMMENT '告警邮箱',
    `webhook_url`      varchar(500)          DEFAULT NULL COMMENT 'WebHook地址',
    `create_time`      datetime              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_job_name_group` (`job_name`,`job_group`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_job_log`
--

DROP TABLE IF EXISTS `sys_job_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_job_log`
(
    `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `job_name`         varchar(200) NOT NULL COMMENT '任务名称',
    `job_group`        varchar(200) NOT NULL COMMENT '任务分组',
    `executor_handler` varchar(200) DEFAULT NULL COMMENT '执行器Handler',
    `job_param`        text COMMENT '任务参数',
    `trigger_time`     datetime     DEFAULT NULL COMMENT '触发时间',
    `start_time`       datetime     DEFAULT NULL COMMENT '开始时间',
    `end_time`         datetime     DEFAULT NULL COMMENT '结束时间',
    `duration`         bigint       DEFAULT NULL COMMENT '执行耗时(ms)',
    `status`           tinyint      NOT NULL COMMENT '状态：1-成功 0-失败',
    `result`           text COMMENT '执行结果',
    `exception_msg`    text COMMENT '异常信息',
    `retry_count`      int          DEFAULT '0' COMMENT '重试次数',
    PRIMARY KEY (`id`),
    KEY                `idx_job_name` (`job_name`),
    KEY                `idx_trigger_time` (`trigger_time`),
    KEY                `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=88 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务执行日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_login_log`
--

DROP TABLE IF EXISTS `sys_login_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_login_log`
(
    `id`             bigint   NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `tenant_id`      bigint            DEFAULT '0' COMMENT '租户编号',
    `user_id`        bigint            DEFAULT NULL COMMENT '用户ID',
    `username`       varchar(50)       DEFAULT NULL COMMENT '用户名',
    `login_type`     varchar(50)       DEFAULT NULL COMMENT '登录类型（LOGIN/LOGOUT/REGISTER）',
    `login_status`   tinyint           DEFAULT '1' COMMENT '登录状态（0-失败，1-成功）',
    `login_ip`       varchar(50)       DEFAULT NULL COMMENT '登录IP',
    `login_location` varchar(255)      DEFAULT NULL COMMENT '登录地点',
    `browser`        varchar(100)      DEFAULT NULL COMMENT '浏览器',
    `os`             varchar(100)      DEFAULT NULL COMMENT '操作系统',
    `user_agent`     varchar(500)      DEFAULT NULL COMMENT '用户代理',
    `login_message`  varchar(500)      DEFAULT NULL COMMENT '登录信息',
    `login_time`     datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    PRIMARY KEY (`id`),
    KEY              `idx_tenant_user` (`tenant_id`,`user_id`),
    KEY              `idx_login_time` (`login_time`),
    KEY              `idx_login_status` (`login_status`)
) ENGINE=InnoDB AUTO_INCREMENT=223 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='登录日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_message`
--

DROP TABLE IF EXISTS `sys_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_message`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`       bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    `title`           varchar(200) NOT NULL COMMENT '消息标题',
    `content`         text         NOT NULL COMMENT '消息内容',
    `type`            varchar(20)  NOT NULL DEFAULT 'SYSTEM' COMMENT '消息类型：SYSTEM-系统消息/SMS-短信/EMAIL-邮件/CUSTOM-自定义',
    `send_scope`      varchar(20)  NOT NULL DEFAULT 'USERS' COMMENT '发送范围：ALL-全员/ORG-指定组织/USERS-指定人员',
    `send_channel`    varchar(20)  NOT NULL DEFAULT 'WEB' COMMENT '发送渠道：WEB-站内信/SMS-短信/EMAIL-邮件/PUSH-推送',
    `status`          tinyint      NOT NULL DEFAULT '0' COMMENT '消息状态：0-草稿/1-已发送/2-发送失败',
    `sender_id`       bigint                DEFAULT NULL COMMENT '发送人ID',
    `sender_name`     varchar(100)          DEFAULT NULL COMMENT '发送人姓名',
    `template_code`   varchar(50)           DEFAULT NULL COMMENT '模板编码',
    `template_params` json                  DEFAULT NULL COMMENT '模板参数JSON',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`       bigint                DEFAULT NULL COMMENT '创建者',
    `update_by`       bigint                DEFAULT NULL COMMENT '更新者',
    `create_dept`     bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    PRIMARY KEY (`id`),
    KEY               `idx_tenant_type` (`tenant_id`,`type`),
    KEY               `idx_status` (`status`),
    KEY               `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统消息主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_message_receiver`
--

DROP TABLE IF EXISTS `sys_message_receiver`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_message_receiver`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`   bigint   NOT NULL DEFAULT '0' COMMENT '租户编号',
    `message_id`  bigint   NOT NULL COMMENT '消息ID',
    `user_id`     bigint   NOT NULL COMMENT '接收人用户ID',
    `org_id`      bigint            DEFAULT NULL COMMENT '接收人所属组织ID',
    `read_flag`   tinyint  NOT NULL DEFAULT '0' COMMENT '已读标记：0-未读/1-已读',
    `read_time`   datetime          DEFAULT NULL COMMENT '阅读时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_message_user` (`message_id`,`user_id`),
    KEY           `idx_user_read` (`user_id`,`read_flag`),
    KEY           `idx_tenant_user` (`tenant_id`,`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息接收人表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_message_send_record`
--

DROP TABLE IF EXISTS `sys_message_send_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_message_send_record`
(
    `id`             bigint      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`      bigint      NOT NULL DEFAULT '0' COMMENT '租户编号',
    `message_id`     bigint      NOT NULL COMMENT '消息ID',
    `channel`        varchar(20) NOT NULL COMMENT '发送渠道：WEB/SMS/EMAIL/PUSH',
    `receiver_count` int                  DEFAULT '0' COMMENT '接收人数量',
    `success_count`  int                  DEFAULT '0' COMMENT '发送成功数量',
    `fail_count`     int                  DEFAULT '0' COMMENT '发送失败数量',
    `external_id`    varchar(100)         DEFAULT NULL COMMENT '第三方渠道返回的消息ID',
    `status`         tinyint     NOT NULL DEFAULT '0' COMMENT '发送状态：0-发送中/1-成功/2-失败',
    `error_msg`      text COMMENT '错误信息',
    `send_time`      datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    `create_time`    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY              `idx_message_id` (`message_id`),
    KEY              `idx_send_time` (`send_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息发送记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_message_template`
--

DROP TABLE IF EXISTS `sys_message_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_message_template`
(
    `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`        bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    `template_code`    varchar(50)  NOT NULL COMMENT '模板编码（唯一）',
    `template_name`    varchar(100) NOT NULL COMMENT '模板名称',
    `type`             varchar(20)  NOT NULL DEFAULT 'SYSTEM' COMMENT '消息类型：SYSTEM/SMS/EMAIL/CUSTOM',
    `title_template`   varchar(200)          DEFAULT NULL COMMENT '标题模板（支持${变量}占位符）',
    `content_template` text         NOT NULL COMMENT '内容模板（支持${变量}占位符）',
    `default_channel`  varchar(20)           DEFAULT 'WEB' COMMENT '默认发送渠道',
    `enabled`          tinyint      NOT NULL DEFAULT '1' COMMENT '是否启用：0-禁用/1-启用',
    `remark`           varchar(500)          DEFAULT NULL COMMENT '备注说明',
    `create_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`        bigint                DEFAULT NULL COMMENT '创建者',
    `update_by`        bigint                DEFAULT NULL COMMENT '更新者',
    `create_dept`      bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_id`,`template_code`),
    KEY                `idx_type` (`type`),
    KEY                `idx_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息模板表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_notice`
--

DROP TABLE IF EXISTS `sys_notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notice`
(
    `notice_id`       bigint       NOT NULL AUTO_INCREMENT COMMENT '公告ID',
    `tenant_id`       bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    `notice_title`    varchar(200) NOT NULL COMMENT '公告标题',
    `notice_content`  text         NOT NULL COMMENT '公告内容',
    `notice_type`     varchar(20)  NOT NULL DEFAULT 'NOTICE' COMMENT '公告类型：NOTICE-通知公告/ANNOUNCEMENT-系统公告/NEWS-新闻动态',
    `publish_status`  tinyint      NOT NULL DEFAULT '0' COMMENT '发布状态：0-草稿/1-已发布/2-已撤回',
    `publish_time`    datetime              DEFAULT NULL COMMENT '发布时间',
    `publisher_id`    bigint                DEFAULT NULL COMMENT '发布人ID',
    `publisher_name`  varchar(100)          DEFAULT NULL COMMENT '发布人姓名',
    `publish_scope`   tinyint      NOT NULL DEFAULT '0' COMMENT '发布范围：0-全部组织/1-指定组织',
    `effective_time`  datetime              DEFAULT NULL COMMENT '生效时间',
    `expiration_time` datetime              DEFAULT NULL COMMENT '失效时间',
    `is_top`          tinyint      NOT NULL DEFAULT '0' COMMENT '是否置顶：0-否/1-是',
    `top_sort`        int                   DEFAULT '0' COMMENT '置顶排序（数字越大越靠前）',
    `attachment_ids`  varchar(500)          DEFAULT NULL COMMENT '附件ID列表（多个附件ID用逗号分隔，关联sys_file_metadata表）',
    `read_count`      int                   DEFAULT '0' COMMENT '阅读次数',
    `remark`          varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`       bigint                DEFAULT NULL COMMENT '创建者',
    `update_by`       bigint                DEFAULT NULL COMMENT '更新者',
    `create_dept`     bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    PRIMARY KEY (`notice_id`),
    KEY               `idx_tenant_id` (`tenant_id`),
    KEY               `idx_publish_status` (`publish_status`),
    KEY               `idx_notice_type` (`notice_type`),
    KEY               `idx_effective_time` (`effective_time`),
    KEY               `idx_expiration_time` (`expiration_time`),
    KEY               `idx_is_top_sort` (`is_top`,`top_sort`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知公告表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_notice_org`
--

DROP TABLE IF EXISTS `sys_notice_org`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notice_org`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `notice_id`   bigint   NOT NULL COMMENT '公告ID',
    `org_id`      bigint   NOT NULL COMMENT '组织ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY           `idx_notice_id` (`notice_id`),
    KEY           `idx_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知公告组织关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_notice_read_record`
--

DROP TABLE IF EXISTS `sys_notice_read_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notice_read_record`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `notice_id`   bigint   NOT NULL COMMENT '公告ID',
    `user_id`     bigint   NOT NULL COMMENT '用户ID',
    `user_name`   varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户姓名',
    `org_id`      bigint                                  DEFAULT NULL COMMENT '用户所属组织ID',
    `org_name`    varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户所属组织名称',
    `read_time`   datetime NOT NULL                       DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
    `create_time` datetime NOT NULL                       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_notice_user` (`notice_id`,`user_id`),
    KEY           `idx_notice_id` (`notice_id`),
    KEY           `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知公告已读记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_operation_log`
--

DROP TABLE IF EXISTS `sys_operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_operation_log`
(
    `id`                 bigint   NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `tenant_id`          bigint            DEFAULT '0' COMMENT '租户编号',
    `user_id`            bigint            DEFAULT NULL COMMENT '操作用户ID',
    `username`           varchar(50)       DEFAULT NULL COMMENT '操作用户名',
    `operation_module`   varchar(100)      DEFAULT NULL COMMENT '操作模块',
    `operation_type`     varchar(50)       DEFAULT NULL COMMENT '操作类型（ADD/UPDATE/DELETE/QUERY/EXPORT/IMPORT）',
    `operation_desc`     varchar(500)      DEFAULT NULL COMMENT '操作描述',
    `request_method`     varchar(10)       DEFAULT NULL COMMENT '请求方式（GET/POST/PUT/DELETE）',
    `request_url`        varchar(500)      DEFAULT NULL COMMENT '请求URL',
    `request_params`     text COMMENT '请求参数',
    `response_result`    text COMMENT '响应结果',
    `error_msg`          text COMMENT '错误信息',
    `operation_status`   tinyint           DEFAULT '1' COMMENT '操作状态（0-失败，1-成功）',
    `operation_ip`       varchar(50)       DEFAULT NULL COMMENT '操作IP',
    `operation_location` varchar(255)      DEFAULT NULL COMMENT '操作地点',
    `user_agent`         varchar(500)      DEFAULT NULL COMMENT '用户代理',
    `execute_time`       bigint            DEFAULT '0' COMMENT '执行时长（毫秒）',
    `operation_time`     datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY                  `idx_tenant_user` (`tenant_id`,`user_id`),
    KEY                  `idx_operation_time` (`operation_time`),
    KEY                  `idx_operation_status` (`operation_status`),
    KEY                  `idx_request_url` (`request_url`(255))
) ENGINE=InnoDB AUTO_INCREMENT=8897 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_org`
--

DROP TABLE IF EXISTS `sys_org`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_org`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '组织ID',
    `tenant_id`   bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    `org_name`    varchar(100) NOT NULL COMMENT '组织名称',
    `parent_id`   bigint       NOT NULL DEFAULT '0' COMMENT '父级组织ID（0为顶级）',
    `ancestors`   varchar(500)          DEFAULT NULL COMMENT '祖级编码（逗号分隔，如：1,2,3）',
    `sort`        int                   DEFAULT '0' COMMENT '排序（值越小越靠前）',
    `org_type`    tinyint               DEFAULT '1' COMMENT '组织类型（1-公司，2-部门，3-小组）',
    `org_status`  tinyint      NOT NULL DEFAULT '1' COMMENT '组织状态（0-禁用，1-正常）',
    `leader_id`   bigint                DEFAULT NULL COMMENT '负责人ID（关联sys_user.id）',
    `leader_name` varchar(50)           DEFAULT NULL COMMENT '负责人姓名',
    `phone`       varchar(20)           DEFAULT NULL COMMENT '组织联系电话',
    `address`     varchar(255)          DEFAULT NULL COMMENT '组织地址',
    `region_code` varchar(30)           DEFAULT NULL COMMENT '行政区划编码',
    `remark`      varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_by`   bigint                DEFAULT NULL COMMENT '创建者',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   bigint                DEFAULT NULL COMMENT '更新者',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_org_name` (`tenant_id`,`org_name`),
    KEY           `idx_tenant_parent` (`tenant_id`,`parent_id`),
    KEY           `idx_org_status` (`org_status`),
    KEY           `idx_ancestors` (`ancestors`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='组织表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_post`
--

DROP TABLE IF EXISTS `sys_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_post`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
    `tenant_id`   bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    `post_code`   varchar(50)  NOT NULL COMMENT '岗位编码（租户内唯一）',
    `org_id`      bigint       NOT NULL COMMENT '所属组织ID（关联sys_org.id）',
    `post_name`   varchar(100) NOT NULL COMMENT '岗位名称',
    `post_status` tinyint      NOT NULL DEFAULT '1' COMMENT '岗位状态（0-禁用，1-正常）',
    `post_type`   tinyint               DEFAULT '1' COMMENT '岗位类型（1-管理岗，2-技术岗，3-业务岗，4-其他）',
    `sort`        int                   DEFAULT '0' COMMENT '排序（值越小越靠前）',
    `remark`      varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_by`   bigint                DEFAULT NULL COMMENT '创建者',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   bigint                DEFAULT NULL COMMENT '更新者',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_post_code` (`tenant_id`,`post_code`),
    UNIQUE KEY `uk_tenant_org_post` (`tenant_id`,`org_id`,`post_name`),
    KEY           `idx_tenant_org` (`tenant_id`,`org_id`),
    KEY           `idx_post_status` (`post_status`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_resource`
--

DROP TABLE IF EXISTS `sys_resource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_resource`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '资源ID',
    `tenant_id`     bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    `resource_name` varchar(100) NOT NULL COMMENT '资源名称',
    `parent_id`     bigint       NOT NULL DEFAULT '0' COMMENT '父级资源ID（0为顶级）',
    `resource_type` tinyint      NOT NULL COMMENT '资源类型（1-目录，2-菜单，3-按钮，4-API接口）',
    `sort`          int                   DEFAULT '0' COMMENT '排序（值越小越靠前）',
    `path`          varchar(255)          DEFAULT NULL COMMENT '资源路由（菜单/目录用）',
    `component`     varchar(255)          DEFAULT NULL COMMENT '前端组件路径（菜单用）',
    `is_external`   tinyint               DEFAULT '0' COMMENT '是否外链（0-否，1-是）',
    `is_public`     tinyint               DEFAULT '0' COMMENT '是否公开资源（0-否，1-是，公开资源无需权限验证）',
    `menu_status`   tinyint               DEFAULT '1' COMMENT '菜单状态（0-隐藏，1-显示，仅菜单/目录用）',
    `visible`       tinyint               DEFAULT '1' COMMENT '显示状态（0-隐藏，1-显示，所有资源通用）',
    `perms`         varchar(100)          DEFAULT NULL COMMENT '权限标识（如：sys:user:list，按钮/API用）',
    `icon`          varchar(50)           DEFAULT NULL COMMENT '图标（菜单/目录用）',
    `api_method`    varchar(10)           DEFAULT NULL COMMENT 'API请求方法（GET/POST/PUT/DELETE，仅API用）',
    `api_url`       varchar(255)          DEFAULT NULL COMMENT 'API接口地址（仅API用）',
    `keep_alive`    tinyint               DEFAULT '0' COMMENT '是否缓存（0-否，1-是，菜单用）',
    `always_show`   tinyint               DEFAULT '0' COMMENT '是否总是显示（0-否，1-是，菜单用）',
    `redirect`      varchar(255)          DEFAULT NULL COMMENT '重定向地址（菜单用）',
    `remark`        varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_by`     bigint                DEFAULT NULL COMMENT '创建者',
    `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`     bigint                DEFAULT NULL COMMENT '更新者',
    `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_dept`   bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_resource` (`tenant_id`,`resource_type`,`perms`) COMMENT '租户内资源权限标识唯一',
    KEY             `idx_tenant_parent` (`tenant_id`,`parent_id`),
    KEY             `idx_resource_type` (`resource_type`),
    KEY             `idx_api_url_method` (`api_url`,`api_method`) COMMENT 'API查询优化'
) ENGINE=InnoDB AUTO_INCREMENT=9047 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统资源表（菜单/按钮/API）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `tenant_id`   bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    `role_name`   varchar(50)  NOT NULL COMMENT '角色名称（租户内唯一）',
    `role_key`    varchar(100) NOT NULL COMMENT '角色权限字符串（如：admin,user:view）',
    `data_scope`  tinyint               DEFAULT '1' COMMENT '权限范围（1-全部数据，2-本租户数据，3-本组织数据，4-本组织及子组织，5-个人数据）',
    `sort`        int                   DEFAULT '0' COMMENT '排序（值越小越靠前）',
    `role_status` tinyint      NOT NULL DEFAULT '1' COMMENT '角色状态（0-禁用，1-正常）',
    `is_system`   tinyint               DEFAULT '0' COMMENT '是否系统内置角色（0-否，1-是）',
    `remark`      varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_by`   bigint                DEFAULT NULL COMMENT '创建者',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   bigint                DEFAULT NULL COMMENT '更新者',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_role_name` (`tenant_id`,`role_name`),
    UNIQUE KEY `uk_tenant_role_key` (`tenant_id`,`role_key`),
    KEY           `idx_tenant_status` (`tenant_id`,`role_status`),
    KEY           `idx_data_scope` (`data_scope`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_role_data_scope`
--

DROP TABLE IF EXISTS `sys_role_data_scope`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_data_scope`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`   bigint   NOT NULL DEFAULT '0' COMMENT '租户编号',
    `role_id`     bigint   NOT NULL COMMENT '角色ID',
    `org_id`      bigint   NOT NULL COMMENT '组织ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_org` (`tenant_id`,`role_id`,`org_id`),
    KEY           `idx_role_id` (`role_id`),
    KEY           `idx_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色-自定义数据权限关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_role_resource`
--

DROP TABLE IF EXISTS `sys_role_resource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_resource`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`   bigint   NOT NULL DEFAULT '0' COMMENT '租户编号',
    `role_id`     bigint   NOT NULL COMMENT '角色ID',
    `resource_id` bigint   NOT NULL COMMENT '资源ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_resource` (`tenant_id`,`role_id`,`resource_id`),
    KEY           `idx_role_id` (`role_id`),
    KEY           `idx_resource_id` (`resource_id`)
) ENGINE=InnoDB AUTO_INCREMENT=346 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色-资源关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_tenant`
--

DROP TABLE IF EXISTS `sys_tenant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_tenant`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '租户ID',
    `tenant_name`    varchar(100) NOT NULL COMMENT '租户名称',
    `contact_person` varchar(50)           DEFAULT NULL COMMENT '负责人',
    `contact_phone`  varchar(20)           DEFAULT NULL COMMENT '联系电话',
    `user_limit`     int                   DEFAULT '0' COMMENT '租户人员数量上限（0表示无限制）',
    `tenant_status`  tinyint      NOT NULL DEFAULT '1' COMMENT '租户状态（0-禁用，1-正常）',
    `expire_time`    datetime              DEFAULT NULL COMMENT '过期时间',
    `tenant_desc`    varchar(500)          DEFAULT NULL COMMENT '租户描述',
    `create_by`      bigint                DEFAULT NULL COMMENT '创建者（系统租户ID）',
    `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`      bigint                DEFAULT NULL COMMENT '更新者',
    `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `browser_icon`   varchar(255)          DEFAULT NULL COMMENT '浏览器icon（存储图标URL/Base64）',
    `browser_title`  varchar(100)          DEFAULT NULL COMMENT '浏览器标签名称',
    `system_name`    varchar(100)          DEFAULT NULL COMMENT '系统名称',
    `system_logo`    varchar(255)          DEFAULT NULL COMMENT '系统logo（存储logo URL/Base64）',
    `system_intro`   varchar(500)          DEFAULT NULL COMMENT '系统介绍',
    `copyright_info` varchar(200)          DEFAULT NULL COMMENT '版权显示文本',
    `system_layout`  varchar(50)           DEFAULT 'default' COMMENT '系统布局（default-默认，classic-经典，modern-现代等）',
    `system_theme`   varchar(50)           DEFAULT 'light' COMMENT '系统主题（light-亮色，dark-暗色，auto-跟随系统等）',
    `theme_config`   varchar(1000)         DEFAULT NULL COMMENT '主题配置',
    `create_dept`    bigint unsigned DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_name` (`tenant_name`),
    KEY              `idx_tenant_status` (`tenant_status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `tenant_id`       bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    `username`        varchar(50)  NOT NULL COMMENT '用户名（租户内唯一）',
    `real_name`       varchar(50)           DEFAULT NULL COMMENT '用户真实姓名',
    `user_type`       tinyint               DEFAULT '1' COMMENT '用户类型（0-系统管理员，1-租户管理员，2-普通用户）',
    `user_client`     varchar(20)           DEFAULT NULL COMMENT '用户触点（app/pc/h5/wechat）',
    `email`           varchar(100)          DEFAULT NULL COMMENT '邮箱',
    `phone`           varchar(20)           DEFAULT NULL COMMENT '手机号',
    `id_card`         varchar(18)           DEFAULT NULL COMMENT '身份证号',
    `gender`          tinyint               DEFAULT '0' COMMENT '性别（0-未知，1-男，2-女）',
    `password`        varchar(100) NOT NULL COMMENT '密码（加密存储）',
    `salt`            varchar(50)           DEFAULT NULL COMMENT '密码盐值',
    `user_status`     tinyint      NOT NULL DEFAULT '1' COMMENT '用户状态（0-禁用，1-正常，2-锁定）',
    `avatar`          varchar(255)          DEFAULT NULL COMMENT '头像URL',
    `last_login_time` datetime              DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip`   varchar(50)           DEFAULT NULL COMMENT '最后登录IP',
    `login_count`     int                   DEFAULT '0' COMMENT '登录次数',
    `remark`          varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_dept`     bigint                DEFAULT NULL COMMENT '创建部门',
    `create_by`       bigint                DEFAULT NULL COMMENT '创建者',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       bigint                DEFAULT NULL COMMENT '更新者',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `sys_user_unique` (`tenant_id`,`username`),
    KEY               `idx_tenant_status` (`tenant_id`,`user_status`),
    KEY               `idx_user_type` (`user_type`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user_org`
--

DROP TABLE IF EXISTS `sys_user_org`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_org`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`   bigint   NOT NULL DEFAULT '0' COMMENT '租户编号',
    `user_id`     bigint   NOT NULL COMMENT '用户ID',
    `org_id`      bigint   NOT NULL COMMENT '组织ID',
    `is_main`     tinyint           DEFAULT '0' COMMENT '是否主组织（0-否，1-是）',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_org` (`tenant_id`,`user_id`,`org_id`),
    KEY           `idx_user_id` (`user_id`),
    KEY           `idx_org_id` (`org_id`),
    KEY           `idx_user_main_org` (`user_id`,`is_main`) COMMENT '查询用户主组织优化'
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户-组织关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user_post`
--

DROP TABLE IF EXISTS `sys_user_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_post`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`   bigint   NOT NULL DEFAULT '0' COMMENT '租户编号',
    `user_id`     bigint   NOT NULL COMMENT '用户ID',
    `post_id`     bigint   NOT NULL COMMENT '岗位ID',
    `is_main`     tinyint           DEFAULT '0' COMMENT '是否主岗（0-否，1-是）',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_post` (`tenant_id`,`user_id`,`post_id`),
    KEY           `idx_user_id` (`user_id`),
    KEY           `idx_post_id` (`post_id`),
    KEY           `idx_user_main_post` (`user_id`,`is_main`) COMMENT '查询用户主岗优化'
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户-岗位关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`   bigint   NOT NULL DEFAULT '0' COMMENT '租户编号',
    `user_id`     bigint   NOT NULL COMMENT '用户ID',
    `role_id`     bigint   NOT NULL COMMENT '角色ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`tenant_id`,`user_id`,`role_id`),
    KEY           `idx_user_id` (`user_id`),
    KEY           `idx_role_id` (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户-角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `worker_node`
--

DROP TABLE IF EXISTS `worker_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `worker_node`
(
    `ID`          bigint      NOT NULL AUTO_INCREMENT COMMENT 'auto increment id',
    `HOST_NAME`   varchar(64) NOT NULL COMMENT 'host name',
    `PORT`        varchar(64) NOT NULL COMMENT 'port',
    `TYPE`        int         NOT NULL COMMENT 'node type: ACTUAL or CONTAINER',
    `LAUNCH_DATE` date        NOT NULL COMMENT 'launch date',
    `MODIFIED`    timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'modified time',
    `CREATED`     timestamp   NOT NULL DEFAULT '0000-00-00 00:00:00' COMMENT 'created time',
    PRIMARY KEY (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=1330 DEFAULT CHARSET=utf8mb3 COMMENT='DB WorkerID Assigner for UID Generator';



INSERT INTO forge_admin_new.sys_api_config (id, api_name, api_code, req_method, url_path, api_version, module_code,
                                            service_id, auth_flag, encrypt_flag, tenant_flag, limit_flag,
                                            sensitive_fields, status, remark, create_by, create_time, update_by,
                                            update_time, tenant_id)
VALUES (723, 'Config-getConfigByKey', 'sysconfig:getConfigByKey', 'GET', '/system/config/configKey/{configKey}',
        'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:42', NULL,
        '2026-02-09 17:03:42', NULL),
       (724, 'OnlineUser-batchKickoutUser', 'sysonlineuser:batchKickoutUser', 'POST', '/auth/online/batchKickout',
        'v1.0.0', 'plugin', NULL, 1, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:42', NULL,
        '2026-02-09 17:03:42', NULL),
       (725, 'DataScopeConfig-remove', 'sysdatascopeconfig:remove', 'POST', '/system/dataScopeConfig/remove', 'v1.0.0',
        'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:42', NULL, '2026-02-09 17:03:42',
        NULL),
       (726, 'Message-getDetail', 'message:getDetail', 'GET', '/api/message/{messageId}', 'v1.0.0', 'plugin', NULL, 0,
        1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:42', NULL, '2026-02-09 17:03:42', NULL),
       (727, 'JobConfig-trigger', 'jobconfig:trigger', 'POST', '/job/config/{id}/trigger', 'v1.0.0', 'plugin', NULL, 0,
        1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:43', NULL, '2026-02-09 17:03:43', NULL),
       (728, 'Post-removeBatch', 'syspost:removeBatch', 'POST', '/system/post/removeBatch', 'v1.0.0', 'plugin', NULL, 0,
        1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:43', NULL, '2026-02-09 17:03:43', NULL),
       (729, 'OnlineUser-banUser', 'sysonlineuser:banUser', 'POST', '/auth/online/ban', 'v1.0.0', 'plugin', NULL, 1, 1,
        1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:43', NULL, '2026-02-09 17:03:43', NULL),
       (730, 'Resource-getById', 'sysresource:getById', 'POST', '/system/resource/getById', 'v1.0.0', 'plugin', NULL, 0,
        1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:43', NULL, '2026-02-09 17:03:43', NULL),
       (731, '删除数据源', 'gendatasource:remove', 'POST', '/generator/datasource/remove/{datasourceId}', 'v1.0.0',
        '数据源管理', NULL, 1, 0, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:43', NULL,
        '2026-02-09 17:03:43', NULL),
       (732, '查询公告统计', 'sysnotice:getStatistics', 'GET', '/system/notice/statistics/{noticeId}', 'v1.0.0',
        '通知公告管理', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:43', NULL,
        '2026-02-09 17:03:43', NULL);
INSERT INTO forge_admin_new.sys_api_config (id, api_name, api_code, req_method, url_path, api_version, module_code,
                                            service_id, auth_flag, encrypt_flag, tenant_flag, limit_flag,
                                            sensitive_fields, status, remark, create_by, create_time, update_by,
                                            update_time, tenant_id)
VALUES (733, 'User-resetPwd', 'sysuser:resetPwd', 'POST', '/system/user/resetPwd', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0,
        NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:43', NULL, '2026-02-09 17:03:43', NULL),
       (734, 'Message-markAllRead', 'message:markAllRead', 'POST', '/api/message/read/all', 'v1.0.0', 'plugin', NULL, 0,
        1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:43', NULL, '2026-02-09 17:03:43', NULL),
       (735, 'DictData-edit', 'sysdictdata:edit', 'POST', '/system/dict/data/edit', 'v1.0.0', 'plugin', NULL, 0, 1, 1,
        0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:43', NULL, '2026-02-09 17:03:43', NULL),
       (736, 'File-getAccessUrl', 'file:getAccessUrl', 'GET', '/api/file/url/{fileId}', 'v1.0.0', 'starter', NULL, 1, 0,
        1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:43', NULL, '2026-02-09 17:03:43', NULL),
       (737, '修改资源', 'sysresource:edit', 'POST', '/system/resource/edit', 'v1.0.0', '资源管理', NULL, 0, 1, 1, 0,
        NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:43', NULL, '2026-02-09 17:03:43', NULL),
       (738, 'Post-list', 'syspost:list', 'GET', '/system/post/list', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:43', NULL, '2026-02-09 17:03:43', NULL),
       (739, 'MessageTemplate-update', 'messagetemplate:update', 'PUT', '/api/message/template', 'v1.0.0', 'plugin',
        NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:43', NULL, '2026-02-09 17:03:43', NULL),
       (740, '重置字段配置', 'gentablecolumn:resetConfig', 'POST', '/generator/column/resetConfig/{tableId}', 'v1.0.0',
        '字段配置', NULL, 1, 0, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:43', NULL, '2026-02-09 17:03:43',
        NULL),
       (741, '导入表结构', 'gen:importTableFromDatasource', 'POST', '/generator/importTable/{datasourceId}', 'v1.0.0',
        '代码生成', NULL, 1, 0, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:43', NULL, '2026-02-09 17:03:43',
        NULL),
       (742, '删除资源', 'sysresource:remove', 'POST', '/system/resource/remove', 'v1.0.0', '资源管理', NULL, 0, 1, 1,
        0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:43', NULL, '2026-02-09 17:03:43', NULL);
INSERT INTO forge_admin_new.sys_api_config (id, api_name, api_code, req_method, url_path, api_version, module_code,
                                            service_id, auth_flag, encrypt_flag, tenant_flag, limit_flag,
                                            sensitive_fields, status, remark, create_by, create_time, update_by,
                                            update_time, tenant_id)
VALUES (743, 'Tenant-selectUserTenantConfig', 'systenant:selectUserTenantConfig', 'POST',
        '/system/tenant/userTenantConfig', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL,
        '2026-02-09 17:03:43', NULL, '2026-02-09 17:03:43', NULL),
       (744, 'MessageTemplate-getById', 'messagetemplate:getById', 'GET', '/api/message/template/{id}', 'v1.0.0',
        'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:43', NULL, '2026-02-09 17:03:43',
        NULL),
       (745, 'Sequence-nextFormatted', 'sequence:nextFormatted', 'GET', '/sequence/nextFormatted', 'v1.0.0', 'starter',
        NULL, 1, 0, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:43', NULL, '2026-02-09 17:03:43', NULL),
       (746, 'Role-getRoleResourceIds', 'sysrole:getRoleResourceIds', 'GET', '/system/role/{roleId}/resources',
        'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:43', NULL,
        '2026-02-09 17:03:43', NULL),
       (747, 'User-doUntieDisable', 'sysuser:doUntieDisable', 'POST', '/system/user/doUntieDisable', 'v1.0.0', 'plugin',
        NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:43', NULL, '2026-02-09 17:03:43', NULL),
       (748, 'JobLog-page', 'joblog:page', 'GET', '/job/log/page', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:43', NULL, '2026-02-09 17:03:43', NULL),
       (749, 'DataScopeConfig-edit', 'sysdatascopeconfig:edit', 'POST', '/system/dataScopeConfig/edit', 'v1.0.0',
        'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:43', NULL, '2026-02-09 17:03:43',
        NULL),
       (750, 'File-initMultipartUpload', 'file:initMultipartUpload', 'POST', '/api/file/multipart/init', 'v1.0.0',
        'starter', NULL, 1, 0, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:43', NULL, '2026-02-09 17:03:43',
        NULL),
       (751, 'JobLog-clean', 'joblog:clean', 'DELETE', '/job/log/clean', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:43', NULL, '2026-02-09 17:03:43', NULL),
       (752, 'KeyExchange-getPublicKey', 'keyexchange:getPublicKey', 'GET', '/crypto/public-key', 'v1.0.0', 'starter',
        NULL, 0, 0, 0, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:43', NULL, '2026-02-09 17:03:43', NULL);
INSERT INTO forge_admin_new.sys_api_config (id, api_name, api_code, req_method, url_path, api_version, module_code,
                                            service_id, auth_flag, encrypt_flag, tenant_flag, limit_flag,
                                            sensitive_fields, status, remark, create_by, create_time, update_by,
                                            update_time, tenant_id)
VALUES (753, 'User-bindOrgs', 'sysuser:bindOrgs', 'POST', '/system/user/{userId}/orgs', 'v1.0.0', 'plugin', NULL, 0, 1,
        1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:43', NULL, '2026-02-09 17:03:43', NULL),
       (754, 'DataScopeConfig-getById', 'sysdatascopeconfig:getById', 'POST', '/system/dataScopeConfig/getById',
        'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:43', NULL,
        '2026-02-09 17:03:43', NULL),
       (755, '查询公告列表', 'sysnotice:list', 'GET', '/system/notice/list', 'v1.0.0', '通知公告管理', NULL, 0, 1, 1, 0,
        NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:43', NULL, '2026-02-09 17:03:43', NULL),
       (756, 'GenericExport-export', 'genericexport:export', 'POST', '/api/excel/export/{configKey}', 'v1.0.0',
        'starter', NULL, 1, 0, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:43', NULL, '2026-02-09 17:03:43',
        NULL),
       (757, 'User-updateStatus', 'sysuser:updateStatus', 'POST', '/system/user/updateStatus', 'v1.0.0', 'plugin', NULL,
        0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:43', NULL, '2026-02-09 17:03:43', NULL),
       (758, 'File-upload', 'file:upload', 'POST', '/api/file/upload', 'v1.0.0', 'starter', NULL, 1, 0, 1, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:43', NULL, '2026-02-09 17:03:43', NULL),
       (759, 'Auth-getUserInfo', 'auth:getUserInfo', 'GET', '/auth/userInfo', 'v1.0.0', 'starter', NULL, 1, 0, 1, 0,
        NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:43', NULL, '2026-02-09 17:03:43', NULL),
       (760, 'File-download', 'file:download', 'GET', '/api/file/download/{fileId}', 'v1.0.0', 'starter', NULL, 1, 0, 1,
        0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44', NULL),
       (761, 'ApiConfigManage-getAllEnabled', 'apiconfigmanage:getAllEnabled', 'GET', '/apiConfig/getAllEnabled',
        'v1.0.0', 'starter', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL,
        '2026-02-09 17:03:44', NULL),
       (762, 'Config-add', 'sysconfig:add', 'POST', '/system/config/add', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44', NULL);
INSERT INTO forge_admin_new.sys_api_config (id, api_name, api_code, req_method, url_path, api_version, module_code,
                                            service_id, auth_flag, encrypt_flag, tenant_flag, limit_flag,
                                            sensitive_fields, status, remark, create_by, create_time, update_by,
                                            update_time, tenant_id)
VALUES (763, 'MessageTemplate-page', 'messagetemplate:page', 'GET', '/api/message/template/page', 'v1.0.0', 'plugin',
        NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44', NULL),
       (764, '清空缓存', 'syscache:clear', 'POST', '/system/cache/clear', 'v1.0.0', '缓存管理', NULL, 0, 1, 1, 0, NULL,
        1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44', NULL),
       (765, '删除API配置', 'sysapiconfig:remove', 'POST', '/system/apiConfig/remove', 'v1.0.0', 'API配置管理', NULL, 1,
        1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44', NULL),
       (766, 'ExcelColumnConfig-add', 'sysexcelcolumnconfig:add', 'POST', '/system/excel/column-config', 'v1.0.0',
        'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44',
        NULL),
       (767, 'File-uploadPart', 'file:uploadPart', 'POST', '/api/file/multipart/upload', 'v1.0.0', 'starter', NULL, 1,
        0, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44', NULL),
       (768, 'ExcelColumnConfig-list', 'sysexcelcolumnconfig:list', 'GET', '/system/excel/column-config/list', 'v1.0.0',
        'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44',
        NULL),
       (769, '修改公告', 'sysnotice:edit', 'POST', '/system/notice/edit', 'v1.0.0', '通知公告管理', NULL, 0, 1, 1, 0,
        NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44', NULL),
       (770, 'OperationLog-detail', 'sysoperationlog:detail', 'GET', '/system/operationLog/{id}', 'v1.0.0', 'plugin',
        NULL, 1, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44', NULL),
       (771, 'Org-getById', 'sysorg:getById', 'POST', '/system/org/getById', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL,
        1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44', NULL),
       (772, 'JobConfig-page', 'jobconfig:page', 'GET', '/job/config/page', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL,
        1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44', NULL);
INSERT INTO forge_admin_new.sys_api_config (id, api_name, api_code, req_method, url_path, api_version, module_code,
                                            service_id, auth_flag, encrypt_flag, tenant_flag, limit_flag,
                                            sensitive_fields, status, remark, create_by, create_time, update_by,
                                            update_time, tenant_id)
VALUES (773, 'Auth-register', 'auth:register', 'POST', '/auth/register', 'v1.0.0', 'starter', NULL, 1, 0, 1, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44', NULL),
       (774, '查询未读用户列表', 'sysnotice:getUnreadUsers', 'GET', '/system/notice/unread-users/{noticeId}', 'v1.0.0',
        '通知公告管理', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL,
        '2026-02-09 17:03:44', NULL),
       (775, 'Org-tree', 'sysorg:tree', 'GET', '/system/org/tree', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44', NULL),
       (776, 'DictType-remove', 'sysdicttype:remove', 'POST', '/system/dict/type/remove', 'v1.0.0', 'plugin', NULL, 0,
        1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44', NULL),
       (777, 'User-bindRoles', 'sysuser:bindRoles', 'POST', '/system/user/{userId}/roles', 'v1.0.0', 'plugin', NULL, 0,
        1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44', NULL),
       (778, '分页查询配置列表', 'sysapiconfig:page', 'GET', '/system/apiConfig/page', 'v1.0.0', 'API配置管理', NULL, 1,
        1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44', NULL),
       (779, 'Post-getById', 'syspost:getById', 'POST', '/system/post/getById', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0,
        NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44', NULL),
       (780, '修改API配置', 'sysapiconfig:edit', 'POST', '/system/apiConfig/edit', 'v1.0.0', 'API配置管理', NULL, 1, 1,
        1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44', NULL),
       (781, 'JobExecutorEndpoint-execute', 'jobexecutorendpoint:execute', 'POST', '/job/executor/execute', 'v1.0.0',
        'plugin', NULL, 1, 0, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44',
        NULL),
       (782, '修改表配置', 'gen:edit', 'POST', '/generator/edit', 'v1.0.0', '代码生成', NULL, 1, 0, 1, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44', NULL);
INSERT INTO forge_admin_new.sys_api_config (id, api_name, api_code, req_method, url_path, api_version, module_code,
                                            service_id, auth_flag, encrypt_flag, tenant_flag, limit_flag,
                                            sensitive_fields, status, remark, create_by, create_time, update_by,
                                            update_time, tenant_id)
VALUES (783, 'Config-getConfig', 'config:getConfig', 'GET', '/config', 'v1.0.0', 'admin', NULL, 1, 0, 1, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44', NULL),
       (784, 'FileStorageConfig-edit', 'sysfilestorageconfig:edit', 'PUT', '/system/storage/config', 'v1.0.0', 'plugin',
        NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44', NULL),
       (785, '查询配置列表', 'sysapiconfig:list', 'GET', '/system/apiConfig/list', 'v1.0.0', 'API配置管理', NULL, 1, 1,
        1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44', NULL),
       (786, 'Tenant-add', 'systenant:add', 'POST', '/system/tenant/add', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44', NULL),
       (787, '查询配置列表', 'sysdatascopeconfig:list', 'GET', '/system/dataScopeConfig/list', 'v1.0.0',
        '数据权限配置管理', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL,
        '2026-02-09 17:03:44', NULL),
       (788, 'DictData-getByType', 'sysdictdata:getByType', 'GET', '/system/dict/data/type/{dictType}', 'v1.0.0',
        'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44',
        NULL),
       (789, 'FileStorageConfig-page', 'sysfilestorageconfig:page', 'GET', '/system/storage/config/page', 'v1.0.0',
        'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44',
        NULL),
       (790, 'FileStorageConfig-detail', 'sysfilestorageconfig:detail', 'POST', '/system/storage/config/detail',
        'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL,
        '2026-02-09 17:03:44', NULL),
       (791, 'OnlineUser-getOnlineUsers', 'sysonlineuser:getOnlineUsers', 'GET', '/auth/online/list', 'v1.0.0',
        'plugin', NULL, 1, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44',
        NULL),
       (792, 'User-unbindRoles', 'sysuser:unbindRoles', 'POST', '/system/user/{userId}/roles/unbind', 'v1.0.0',
        'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44',
        NULL);
INSERT INTO forge_admin_new.sys_api_config (id, api_name, api_code, req_method, url_path, api_version, module_code,
                                            service_id, auth_flag, encrypt_flag, tenant_flag, limit_flag,
                                            sensitive_fields, status, remark, create_by, create_time, update_by,
                                            update_time, tenant_id)
VALUES (793, 'OnlineUser-getOnlineUsersPage', 'sysonlineuser:getOnlineUsersPage', 'GET', '/auth/online/page', 'v1.0.0',
        'plugin', NULL, 1, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44',
        NULL),
       (794, 'GenTemplate-enabledList', 'gentemplate:enabledList', 'GET', '/generator/template/enabled', 'v1.0.0',
        'plugin', NULL, 1, 0, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44',
        NULL),
       (795, 'Notice-userPage', 'sysnotice:userPage', 'GET', '/system/notice/user/page', 'v1.0.0', 'plugin', NULL, 0, 1,
        1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44', NULL),
       (796, 'ExcelExportConfig-copy', 'sysexcelexportconfig:copy', 'POST', '/system/excel/export-config/copy',
        'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL,
        '2026-02-09 17:03:44', NULL),
       (797, 'User-updateProfile', 'sysuser:updateProfile', 'POST', '/system/user/updateProfile', 'v1.0.0', 'plugin',
        NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44', NULL),
       (798, 'LoginLog-page', 'sysloginlog:page', 'GET', '/system/loginLog/page', 'v1.0.0', 'plugin', NULL, 1, 1, 1, 0,
        NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44', NULL),
       (799, 'User-getUserOrgIds', 'sysuser:getUserOrgIds', 'GET', '/system/user/{userId}/orgs', 'v1.0.0', 'plugin',
        NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44', NULL),
       (800, 'DataScope-refreshCache', 'datascope:refreshCache', 'POST', '/datascope/refreshCache', 'v1.0.0', 'starter',
        NULL, 1, 0, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:44', NULL, '2026-02-09 17:03:44', NULL),
       (801, 'GenTableColumn-list', 'gentablecolumn:list', 'GET', '/generator/column/list/{tableId}', 'v1.0.0',
        'plugin', NULL, 1, 0, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:45', NULL, '2026-02-09 17:03:45',
        NULL),
       (802, 'Auth-refreshToken', 'auth:refreshToken', 'POST', '/auth/refreshToken', 'v1.0.0', 'starter', NULL, 1, 0, 1,
        0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:45', NULL, '2026-02-09 17:03:45', NULL);
INSERT INTO forge_admin_new.sys_api_config (id, api_name, api_code, req_method, url_path, api_version, module_code,
                                            service_id, auth_flag, encrypt_flag, tenant_flag, limit_flag,
                                            sensitive_fields, status, remark, create_by, create_time, update_by,
                                            update_time, tenant_id)
VALUES (803, '新增模板', 'gentemplate:add', 'POST', '/generator/template/add', 'v1.0.0', '模板管理', NULL, 1, 0, 1, 0,
        NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:45', NULL, '2026-02-09 17:03:45', NULL),
       (804, 'JobConfig-delete', 'jobconfig:delete', 'DELETE', '/job/config/{id}', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0,
        NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:45', NULL, '2026-02-09 17:03:45', NULL),
       (805, '新增资源', 'sysresource:add', 'POST', '/system/resource/add', 'v1.0.0', '资源管理', NULL, 0, 1, 1, 0,
        NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:45', NULL, '2026-02-09 17:03:45', NULL),
       (806, '批量删除API配置', 'sysapiconfig:removeBatch', 'POST', '/system/apiConfig/removeBatch', 'v1.0.0',
        'API配置管理', NULL, 1, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:45', NULL,
        '2026-02-09 17:03:45', NULL),
       (807, 'GenDatasource-getInfo', 'gendatasource:getInfo', 'GET', '/generator/datasource/{datasourceId}', 'v1.0.0',
        'plugin', NULL, 1, 0, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:45', NULL, '2026-02-09 17:03:45',
        NULL),
       (808, 'GenDatasource-getTables', 'gendatasource:getTables', 'GET', '/generator/datasource/{datasourceId}/tables',
        'v1.0.0', 'plugin', NULL, 1, 0, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:45', NULL,
        '2026-02-09 17:03:45', NULL),
       (809, 'Message-send', 'message:send', 'POST', '/api/message/send', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:45', NULL, '2026-02-09 17:03:45', NULL),
       (810, '新增API配置', 'sysapiconfig:add', 'POST', '/system/apiConfig/add', 'v1.0.0', 'API配置管理', NULL, 1, 1, 1,
        0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:45', NULL, '2026-02-09 17:03:45', NULL),
       (811, 'Resource-getCurrentUserMenuTree', 'sysresource:getCurrentUserMenuTree', 'GET',
        '/system/resource/current/menu', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL,
        '2026-02-09 17:03:45', NULL, '2026-02-09 17:03:45', NULL),
       (812, 'Message-page', 'message:page', 'POST', '/api/message/page', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:45', NULL, '2026-02-09 17:03:45', NULL);
INSERT INTO forge_admin_new.sys_api_config (id, api_name, api_code, req_method, url_path, api_version, module_code,
                                            service_id, auth_flag, encrypt_flag, tenant_flag, limit_flag,
                                            sensitive_fields, status, remark, create_by, create_time, update_by,
                                            update_time, tenant_id)
VALUES (813, 'Post-add', 'syspost:add', 'POST', '/system/post/add', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:45', NULL, '2026-02-09 17:03:45', NULL),
       (814, '导入表结构', 'gen:importTable', 'POST', '/generator/importTable', 'v1.0.0', '代码生成', NULL, 1, 0, 1, 0,
        NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:45', NULL, '2026-02-09 17:03:45', NULL),
       (815, 'JobConfig-updateCron', 'jobconfig:updateCron', 'POST', '/job/config/{id}/cron', 'v1.0.0', 'plugin', NULL,
        0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:45', NULL, '2026-02-09 17:03:45', NULL),
       (816, '查询数据库表列表', 'gen:dbList', 'GET', '/generator/db/list', 'v1.0.0', '代码生成', NULL, 1, 0, 1, 0,
        NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:45', NULL, '2026-02-09 17:03:45', NULL),
       (817, '分页查询生成表列表', 'gen:list', 'GET', '/generator/list', 'v1.0.0', '代码生成', NULL, 1, 0, 1, 0, NULL,
        1, '系统自动注册', NULL, '2026-02-09 17:03:45', NULL, '2026-02-09 17:03:45', NULL),
       (818, 'User-unbindOrg', 'sysuser:unbindOrg', 'POST', '/system/user/{userId}/org/unbind', 'v1.0.0', 'plugin',
        NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:45', NULL, '2026-02-09 17:03:45', NULL),
       (819, 'FileMetadata-detail', 'sysfilemetadata:detail', 'GET', '/system/file/metadata/{id}', 'v1.0.0', 'plugin',
        NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:45', NULL, '2026-02-09 17:03:45', NULL),
       (820, 'DictData-getById', 'sysdictdata:getById', 'POST', '/system/dict/data/getById', 'v1.0.0', 'plugin', NULL,
        0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:45', NULL, '2026-02-09 17:03:45', NULL),
       (821, 'ExcelColumnConfig-detail', 'sysexcelcolumnconfig:detail', 'POST', '/system/excel/column-config/detail',
        'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:45', NULL,
        '2026-02-09 17:03:45', NULL),
       (822, 'Role-add', 'sysrole:add', 'POST', '/system/role/add', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:45', NULL, '2026-02-09 17:03:45', NULL);
INSERT INTO forge_admin_new.sys_api_config (id, api_name, api_code, req_method, url_path, api_version, module_code,
                                            service_id, auth_flag, encrypt_flag, tenant_flag, limit_flag,
                                            sensitive_fields, status, remark, create_by, create_time, update_by,
                                            update_time, tenant_id)
VALUES (823, 'FileMetadata-listByBusiness', 'sysfilemetadata:listByBusiness', 'GET',
        '/system/file/metadata/business/{businessType}/{businessId}', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:45', NULL, '2026-02-09 17:03:45', NULL),
       (824, 'GenTemplate-getTypes', 'gentemplate:getTypes', 'GET', '/generator/template/types', 'v1.0.0', 'plugin',
        NULL, 1, 0, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:45', NULL, '2026-02-09 17:03:45', NULL),
       (825, 'User-remove', 'sysuser:remove', 'POST', '/system/user/remove', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL,
        1, '系统自动注册', NULL, '2026-02-09 17:03:45', NULL, '2026-02-09 17:03:45', NULL),
       (826, 'Config-remove', 'sysconfig:remove', 'POST', '/system/config/remove', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0,
        NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:45', NULL, '2026-02-09 17:03:45', NULL),
       (827, 'ConfigRefresh-refresh', 'configrefresh:refresh', 'POST', '/api/config/refresh', 'v1.0.0', 'starter', NULL,
        1, 0, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:45', NULL, '2026-02-09 17:03:45', NULL),
       (828, 'Tenant-edit', 'systenant:edit', 'POST', '/system/tenant/edit', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL,
        1, '系统自动注册', NULL, '2026-02-09 17:03:45', NULL, '2026-02-09 17:03:45', NULL),
       (829, '分页查询数据源列表', 'gendatasource:list', 'GET', '/generator/datasource/list', 'v1.0.0', '数据源管理',
        NULL, 1, 0, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:45', NULL, '2026-02-09 17:03:45', NULL),
       (830, 'Auth-login', 'auth:login', 'POST', '/auth/login', 'v1.0.0', 'starter', NULL, 1, 0, 0, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:45', NULL, '2026-02-09 17:03:45', NULL),
       (831, 'Role-page', 'sysrole:page', 'GET', '/system/role/page', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:45', NULL, '2026-02-09 17:03:45', NULL),
       (832, 'GenericExport-exportByGet', 'genericexport:exportByGet', 'GET', '/api/excel/export/{configKey}', 'v1.0.0',
        'starter', NULL, 1, 0, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:45', NULL, '2026-02-09 17:03:45',
        NULL);
INSERT INTO forge_admin_new.sys_api_config (id, api_name, api_code, req_method, url_path, api_version, module_code,
                                            service_id, auth_flag, encrypt_flag, tenant_flag, limit_flag,
                                            sensitive_fields, status, remark, create_by, create_time, update_by,
                                            update_time, tenant_id)
VALUES (833, 'Resource-tree', 'sysresource:tree', 'GET', '/system/resource/tree', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0,
        NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:45', NULL, '2026-02-09 17:03:45', NULL),
       (834, 'User-removeBatch', 'sysuser:removeBatch', 'POST', '/system/user/removeBatch', 'v1.0.0', 'plugin', NULL, 0,
        1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:45', NULL, '2026-02-09 17:03:45', NULL),
       (835, '修改数据源', 'gendatasource:edit', 'POST', '/generator/datasource/edit', 'v1.0.0', '数据源管理', NULL, 1,
        0, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:46', NULL, '2026-02-09 17:03:46', NULL),
       (836, '自动注册API配置', 'apiconfigmanage:registerApiConfigs', 'POST', '/apiConfig/registerApiConfigs', 'v1.0.0',
        'API配置管理', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:46', NULL,
        '2026-02-09 17:03:46', NULL),
       (837, 'Message-markReadBatch', 'message:markReadBatch', 'POST', '/api/message/read/batch', 'v1.0.0', 'plugin',
        NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:46', NULL, '2026-02-09 17:03:46', NULL),
       (838, 'ApiConfigManage-getCacheStats', 'apiconfigmanage:getCacheStats', 'GET', '/apiConfig/cacheStats', 'v1.0.0',
        'starter', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:46', NULL, '2026-02-09 17:03:46',
        NULL),
       (839, '清空所有API配置缓存', 'apiconfigmanage:clearCache', 'POST', '/apiConfig/clearCache', 'v1.0.0',
        'API配置管理', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:46', NULL,
        '2026-02-09 17:03:46', NULL),
       (840, 'Resource-getCurrentUserResourceTree', 'sysresource:getCurrentUserResourceTree', 'GET',
        '/system/resource/current/tree', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL,
        '2026-02-09 17:03:46', NULL, '2026-02-09 17:03:46', NULL),
       (841, 'Gen-preview', 'gen:preview', 'GET', '/generator/preview/{tableName}', 'v1.0.0', 'plugin', NULL, 1, 0, 1,
        0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:46', NULL, '2026-02-09 17:03:46', NULL),
       (842, 'ExcelColumnConfig-edit', 'sysexcelcolumnconfig:edit', 'PUT', '/system/excel/column-config', 'v1.0.0',
        'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:46', NULL, '2026-02-09 17:03:46',
        NULL);
INSERT INTO forge_admin_new.sys_api_config (id, api_name, api_code, req_method, url_path, api_version, module_code,
                                            service_id, auth_flag, encrypt_flag, tenant_flag, limit_flag,
                                            sensitive_fields, status, remark, create_by, create_time, update_by,
                                            update_time, tenant_id)
VALUES (843, 'Role-getById', 'sysrole:getById', 'POST', '/system/role/getById', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0,
        NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:46', NULL, '2026-02-09 17:03:46', NULL),
       (844, 'Gen-getInfo', 'gen:getInfo', 'GET', '/generator/{tableId}', 'v1.0.0', 'plugin', NULL, 1, 0, 1, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:46', NULL, '2026-02-09 17:03:46', NULL),
       (845, 'Role-removeBatch', 'sysrole:removeBatch', 'POST', '/system/role/removeBatch', 'v1.0.0', 'plugin', NULL, 0,
        1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:46', NULL, '2026-02-09 17:03:46', NULL),
       (846, '删除缓存', 'syscache:remove', 'POST', '/system/cache/remove', 'v1.0.0', '缓存管理', NULL, 0, 1, 1, 0,
        NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:46', NULL, '2026-02-09 17:03:46', NULL),
       (847, 'DictData-remove', 'sysdictdata:remove', 'POST', '/system/dict/data/remove', 'v1.0.0', 'plugin', NULL, 0,
        1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:46', NULL, '2026-02-09 17:03:46', NULL),
       (848, '批量删除公告', 'sysnotice:removeBatch', 'POST', '/system/notice/removeBatch', 'v1.0.0', '通知公告管理',
        NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:46', NULL, '2026-02-09 17:03:46', NULL),
       (849, 'Role-bindResources', 'sysrole:bindResources', 'POST', '/system/role/{roleId}/resources', 'v1.0.0',
        'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:46', NULL, '2026-02-09 17:03:46',
        NULL),
       (850, 'File-completeMultipartUpload', 'file:completeMultipartUpload', 'POST', '/api/file/multipart/complete',
        'v1.0.0', 'starter', NULL, 1, 0, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:46', NULL,
        '2026-02-09 17:03:46', NULL),
       (851, 'MessageTemplate-delete', 'messagetemplate:delete', 'DELETE', '/api/message/template/{id}', 'v1.0.0',
        'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:46', NULL, '2026-02-09 17:03:46',
        NULL),
       (852, 'Config-removeBatch', 'sysconfig:removeBatch', 'POST', '/system/config/removeBatch', 'v1.0.0', 'plugin',
        NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:46', NULL, '2026-02-09 17:03:46', NULL);
INSERT INTO forge_admin_new.sys_api_config (id, api_name, api_code, req_method, url_path, api_version, module_code,
                                            service_id, auth_flag, encrypt_flag, tenant_flag, limit_flag,
                                            sensitive_fields, status, remark, create_by, create_time, update_by,
                                            update_time, tenant_id)
VALUES (853, 'Org-page', 'sysorg:page', 'GET', '/system/org/page', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:46', NULL, '2026-02-09 17:03:46', NULL),
       (854, 'Notice-markAsRead', 'sysnotice:markAsRead', 'POST', '/system/notice/markAsRead', 'v1.0.0', 'plugin', NULL,
        0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:46', NULL, '2026-02-09 17:03:46', NULL),
       (855, 'Auth-logout', 'auth:logout', 'POST', '/auth/logout', 'v1.0.0', 'starter', NULL, 1, 0, 0, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:46', NULL, '2026-02-09 17:03:46', NULL),
       (856, 'Org-add', 'sysorg:add', 'POST', '/system/org/add', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:46', NULL, '2026-02-09 17:03:46', NULL),
       (857, 'File-delete', 'file:delete', 'DELETE', '/api/file/{fileId}', 'v1.0.0', 'starter', NULL, 1, 0, 1, 0, NULL,
        1, '系统自动注册', NULL, '2026-02-09 17:03:46', NULL, '2026-02-09 17:03:46', NULL),
       (858, 'Gen-batchDownload', 'gen:batchDownload', 'POST', '/generator/batchDownload', 'v1.0.0', 'plugin', NULL, 1,
        0, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:46', NULL, '2026-02-09 17:03:46', NULL),
       (859, 'Tenant-getById', 'systenant:getById', 'POST', '/system/tenant/getById', 'v1.0.0', 'plugin', NULL, 0, 1, 1,
        0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:46', NULL, '2026-02-09 17:03:46', NULL),
       (860, 'FileStorageConfig-add', 'sysfilestorageconfig:add', 'POST', '/system/storage/config', 'v1.0.0', 'plugin',
        NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:46', NULL, '2026-02-09 17:03:46', NULL),
       (861, '发布公告', 'sysnotice:publish', 'POST', '/system/notice/publish', 'v1.0.0', '通知公告管理', NULL, 0, 1, 1,
        0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:46', NULL, '2026-02-09 17:03:46', NULL),
       (862, '删除表配置', 'gen:remove', 'POST', '/generator/remove', 'v1.0.0', '代码生成', NULL, 1, 0, 1, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:46', NULL, '2026-02-09 17:03:46', NULL);
INSERT INTO forge_admin_new.sys_api_config (id, api_name, api_code, req_method, url_path, api_version, module_code,
                                            service_id, auth_flag, encrypt_flag, tenant_flag, limit_flag,
                                            sensitive_fields, status, remark, create_by, create_time, update_by,
                                            update_time, tenant_id)
VALUES (863, 'Tenant-remove', 'systenant:remove', 'POST', '/system/tenant/remove', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0,
        NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:46', NULL, '2026-02-09 17:03:46', NULL),
       (864, 'Message-markRead', 'message:markRead', 'POST', '/api/message/{messageId}/read', 'v1.0.0', 'plugin', NULL,
        0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:47', NULL, '2026-02-09 17:03:47', NULL),
       (865, 'OnlineUser-banUser', 'sysonlineuser:banUser', 'GET', '/auth/online/test', 'v1.0.0', 'plugin', NULL, 1, 1,
        0, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:47', NULL, '2026-02-09 17:03:47', NULL),
       (866, 'User-add', 'sysuser:add', 'POST', '/system/user/add', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:47', NULL, '2026-02-09 17:03:47', NULL),
       (867, '修改模板', 'gentemplate:edit', 'POST', '/generator/template/edit', 'v1.0.0', '模板管理', NULL, 1, 0, 1, 0,
        NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:47', NULL, '2026-02-09 17:03:47', NULL),
       (868, 'MessageTemplate-getByCode', 'messagetemplate:getByCode', 'GET',
        '/api/message/template/code/{templateCode}', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册',
        NULL, '2026-02-09 17:03:47', NULL, '2026-02-09 17:03:47', NULL),
       (869, 'Post-page', 'syspost:page', 'GET', '/system/post/page', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:47', NULL, '2026-02-09 17:03:47', NULL),
       (870, 'Org-remove', 'sysorg:remove', 'POST', '/system/org/remove', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:47', NULL, '2026-02-09 17:03:47', NULL),
       (871, '测试数据源连接', 'gendatasource:testConnection', 'POST', '/generator/datasource/test/{datasourceId}',
        'v1.0.0', '数据源管理', NULL, 1, 0, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:47', NULL,
        '2026-02-09 17:03:47', NULL),
       (872, 'ExcelExportConfig-edit', 'sysexcelexportconfig:edit', 'PUT', '/system/excel/export-config', 'v1.0.0',
        'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:47', NULL, '2026-02-09 17:03:47',
        NULL);
INSERT INTO forge_admin_new.sys_api_config (id, api_name, api_code, req_method, url_path, api_version, module_code,
                                            service_id, auth_flag, encrypt_flag, tenant_flag, limit_flag,
                                            sensitive_fields, status, remark, create_by, create_time, update_by,
                                            update_time, tenant_id)
VALUES (873, '查询已读用户列表', 'sysnotice:getReadUsers', 'GET', '/system/notice/read-users/{noticeId}', 'v1.0.0',
        '通知公告管理', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:47', NULL,
        '2026-02-09 17:03:47', NULL),
       (874, 'DataScopeConfig-add', 'sysdatascopeconfig:add', 'POST', '/system/dataScopeConfig/add', 'v1.0.0', 'plugin',
        NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:47', NULL, '2026-02-09 17:03:47', NULL),
       (875, 'BasicError-errorHtml', 'basicerror:errorHtml', 'ALL', '/error', 'v1.0.0', 'autoconfigure', NULL, 1, 0, 1,
        0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:47', NULL, '2026-02-09 17:03:47', NULL),
       (876, 'DictType-getById', 'sysdicttype:getById', 'POST', '/system/dict/type/getById', 'v1.0.0', 'plugin', NULL,
        0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:47', NULL, '2026-02-09 17:03:47', NULL),
       (877, 'ExcelExportConfig-testExport', 'sysexcelexportconfig:testExport', 'GET',
        '/system/excel/export-config/test/{id}', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL,
        '2026-02-09 17:03:47', NULL, '2026-02-09 17:03:47', NULL),
       (878, '删除模板', 'gentemplate:remove', 'POST', '/generator/template/remove/{templateId}', 'v1.0.0', '模板管理',
        NULL, 1, 0, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:47', NULL, '2026-02-09 17:03:47', NULL),
       (879, 'Resource-getCurrentUserPermissions', 'sysresource:getCurrentUserPermissions', 'GET',
        '/system/resource/current/permissions', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL,
        '2026-02-09 17:03:47', NULL, '2026-02-09 17:03:47', NULL),
       (880, '分页查询公告列表', 'sysnotice:page', 'GET', '/system/notice/page', 'v1.0.0', '通知公告管理', NULL, 0, 1,
        1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:47', NULL, '2026-02-09 17:03:47', NULL),
       (881, 'DictType-removeBatch', 'sysdicttype:removeBatch', 'POST', '/system/dict/type/removeBatch', 'v1.0.0',
        'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:47', NULL, '2026-02-09 17:03:47',
        NULL),
       (882, '刷新所有API配置缓存', 'apiconfigmanage:refreshAll', 'POST', '/apiConfig/refresh', 'v1.0.0', 'API配置管理',
        NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:47', NULL, '2026-02-09 17:03:47', NULL);
INSERT INTO forge_admin_new.sys_api_config (id, api_name, api_code, req_method, url_path, api_version, module_code,
                                            service_id, auth_flag, encrypt_flag, tenant_flag, limit_flag,
                                            sensitive_fields, status, remark, create_by, create_time, update_by,
                                            update_time, tenant_id)
VALUES (883, '分页查询模板列表', 'gentemplate:list', 'GET', '/generator/template/list', 'v1.0.0', '模板管理', NULL, 1,
        0, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:47', NULL, '2026-02-09 17:03:47', NULL),
       (884, 'OnlineUser-unbanUser', 'sysonlineuser:unbanUser', 'POST', '/auth/online/unban', 'v1.0.0', 'plugin', NULL,
        1, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:47', NULL, '2026-02-09 17:03:47', NULL),
       (885, 'DictData-page', 'sysdictdata:page', 'GET', '/system/dict/data/page', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0,
        NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:47', NULL, '2026-02-09 17:03:47', NULL),
       (886, 'User-bindOrg', 'sysuser:bindOrg', 'POST', '/system/user/{userId}/org', 'v1.0.0', 'plugin', NULL, 0, 1, 1,
        0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:47', NULL, '2026-02-09 17:03:47', NULL),
       (887, 'ExcelColumnConfig-remove', 'sysexcelcolumnconfig:remove', 'DELETE', '/system/excel/column-config/{ids}',
        'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:47', NULL,
        '2026-02-09 17:03:47', NULL),
       (888, 'Sequence-next', 'sequence:next', 'GET', '/sequence/next', 'v1.0.0', 'starter', NULL, 1, 0, 1, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:47', NULL, '2026-02-09 17:03:47', NULL),
       (889, 'User-getById', 'sysuser:getById', 'POST', '/system/user/getById', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0,
        NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:48', NULL, '2026-02-09 17:03:48', NULL),
       (890, 'GenTemplate-getInfo', 'gentemplate:getInfo', 'GET', '/generator/template/{templateId}', 'v1.0.0',
        'plugin', NULL, 1, 0, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:48', NULL, '2026-02-09 17:03:48',
        NULL),
       (891, 'DictData-add', 'sysdictdata:add', 'POST', '/system/dict/data/add', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0,
        NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:48', NULL, '2026-02-09 17:03:48', NULL),
       (892, 'OperationLog-page', 'sysoperationlog:page', 'GET', '/system/operationLog/page', 'v1.0.0', 'plugin', NULL,
        1, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:48', NULL, '2026-02-09 17:03:48', NULL);
INSERT INTO forge_admin_new.sys_api_config (id, api_name, api_code, req_method, url_path, api_version, module_code,
                                            service_id, auth_flag, encrypt_flag, tenant_flag, limit_flag,
                                            sensitive_fields, status, remark, create_by, create_time, update_by,
                                            update_time, tenant_id)
VALUES (893, 'Resource-page', 'sysresource:page', 'GET', '/system/resource/page', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0,
        NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:48', NULL, '2026-02-09 17:03:48', NULL),
       (894, '删除公告', 'sysnotice:remove', 'POST', '/system/notice/remove', 'v1.0.0', '通知公告管理', NULL, 0, 1, 1,
        0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:48', NULL, '2026-02-09 17:03:48', NULL),
       (895, 'Auth-changePassword', 'auth:changePassword', 'POST', '/auth/changePassword', 'v1.0.0', 'starter', NULL, 1,
        0, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:48', NULL, '2026-02-09 17:03:48', NULL),
       (896, 'Message-getUnreadCount', 'message:getUnreadCount', 'GET', '/api/message/unread/count', 'v1.0.0', 'plugin',
        NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:48', NULL, '2026-02-09 17:03:48', NULL),
       (897, 'FileMetadata-remove', 'sysfilemetadata:remove', 'DELETE', '/system/file/metadata/{fileIds}', 'v1.0.0',
        'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:48', NULL, '2026-02-09 17:03:48',
        NULL),
       (898, 'JobConfig-stop', 'jobconfig:stop', 'POST', '/job/config/{id}/stop', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0,
        NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:48', NULL, '2026-02-09 17:03:48', NULL),
       (899, '新增公告', 'sysnotice:add', 'POST', '/system/notice/add', 'v1.0.0', '通知公告管理', NULL, 0, 1, 1, 0,
        NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:48', NULL, '2026-02-09 17:03:48', NULL),
       (900, 'MessageTemplate-create', 'messagetemplate:create', 'POST', '/api/message/template', 'v1.0.0', 'plugin',
        NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:48', NULL, '2026-02-09 17:03:48', NULL),
       (901, '查询公告详情', 'sysnotice:getById', 'POST', '/system/notice/getById', 'v1.0.0', '通知公告管理', NULL, 0,
        1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:48', NULL, '2026-02-09 17:03:48', NULL),
       (902, 'DictData-list', 'sysdictdata:list', 'GET', '/system/dict/data/list', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0,
        NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:48', NULL, '2026-02-09 17:03:48', NULL);
INSERT INTO forge_admin_new.sys_api_config (id, api_name, api_code, req_method, url_path, api_version, module_code,
                                            service_id, auth_flag, encrypt_flag, tenant_flag, limit_flag,
                                            sensitive_fields, status, remark, create_by, create_time, update_by,
                                            update_time, tenant_id)
VALUES (903, '分页查询配置列表', 'sysconfig:page', 'GET', '/system/config/page', 'v1.0.0', '系统配置管理', NULL, 0, 1,
        1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:48', NULL, '2026-02-09 17:03:48', NULL),
       (904, 'FileStorageConfig-updateEnabled', 'sysfilestorageconfig:updateEnabled', 'PUT',
        '/system/storage/config/enabled/{id}/{enabled}', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册',
        NULL, '2026-02-09 17:03:48', NULL, '2026-02-09 17:03:48', NULL),
       (905, 'Id-next', 'id:next', 'GET', '/id/next', 'v1.0.0', 'starter', NULL, 1, 0, 1, 0, NULL, 1, '系统自动注册',
        NULL, '2026-02-09 17:03:48', NULL, '2026-02-09 17:03:48', NULL),
       (906, '撤回公告', 'sysnotice:revoke', 'POST', '/system/notice/revoke', 'v1.0.0', '通知公告管理', NULL, 0, 1, 1,
        0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:48', NULL, '2026-02-09 17:03:48', NULL),
       (907, 'Gen-download', 'gen:download', 'GET', '/generator/download/{tableName}', 'v1.0.0', 'plugin', NULL, 1, 0,
        1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:48', NULL, '2026-02-09 17:03:48', NULL),
       (908, '分页查询配置列表', 'sysdatascopeconfig:page', 'GET', '/system/dataScopeConfig/page', 'v1.0.0',
        '数据权限配置管理', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:49', NULL,
        '2026-02-09 17:03:49', NULL),
       (909, 'FileStorageConfig-testConnection', 'sysfilestorageconfig:testConnection', 'POST',
        '/system/storage/config/test/{id}', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL,
        '2026-02-09 17:03:48', NULL, '2026-02-09 17:03:48', NULL),
       (910, 'GenDatasource-enabledList', 'gendatasource:enabledList', 'GET', '/generator/datasource/enabled', 'v1.0.0',
        'plugin', NULL, 1, 0, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:49', NULL, '2026-02-09 17:03:49',
        NULL),
       (911, 'JobConfig-update', 'jobconfig:update', 'PUT', '/job/config', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL,
        1, '系统自动注册', NULL, '2026-02-09 17:03:49', NULL, '2026-02-09 17:03:49', NULL),
       (912, 'FileStorageConfig-setDefault', 'sysfilestorageconfig:setDefault', 'PUT',
        '/system/storage/config/default/{id}', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL,
        '2026-02-09 17:03:49', NULL, '2026-02-09 17:03:49', NULL);
INSERT INTO forge_admin_new.sys_api_config (id, api_name, api_code, req_method, url_path, api_version, module_code,
                                            service_id, auth_flag, encrypt_flag, tenant_flag, limit_flag,
                                            sensitive_fields, status, remark, create_by, create_time, update_by,
                                            update_time, tenant_id)
VALUES (913, 'FileStorageConfig-remove', 'sysfilestorageconfig:remove', 'DELETE', '/system/storage/config/{ids}',
        'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:49', NULL,
        '2026-02-09 17:03:49', NULL),
       (914, 'JobConfig-detail', 'jobconfig:detail', 'GET', '/job/config/{id}', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0,
        NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:49', NULL, '2026-02-09 17:03:49', NULL),
       (915, '查询配置列表', 'sysconfig:list', 'GET', '/system/config/list', 'v1.0.0', '系统配置管理', NULL, 0, 1, 1, 0,
        NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:49', NULL, '2026-02-09 17:03:49', NULL),
       (916, 'JobLog-detail', 'joblog:detail', 'GET', '/job/log/{id}', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:49', NULL, '2026-02-09 17:03:49', NULL),
       (917, 'FileMetadata-page', 'sysfilemetadata:page', 'GET', '/system/file/metadata/page', 'v1.0.0', 'plugin', NULL,
        0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:49', NULL, '2026-02-09 17:03:49', NULL),
       (918, 'User-page', 'sysuser:page', 'GET', '/system/user/page', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:49', NULL, '2026-02-09 17:03:49', NULL),
       (919, 'Config-getById', 'sysconfig:getById', 'POST', '/system/config/getById', 'v1.0.0', 'plugin', NULL, 0, 1, 1,
        0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:49', NULL, '2026-02-09 17:03:49', NULL),
       (920, 'OnlineUser-getUserTokens', 'sysonlineuser:getUserTokens', 'GET', '/auth/online/userTokens', 'v1.0.0',
        'plugin', NULL, 1, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:49', NULL, '2026-02-09 17:03:49',
        NULL),
       (921, 'Sequence-nextFormattedBatch', 'sequence:nextFormattedBatch', 'GET', '/sequence/nextFormattedBatch',
        'v1.0.0', 'starter', NULL, 1, 0, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:49', NULL,
        '2026-02-09 17:03:49', NULL),
       (922, '批量更新字段配置', 'gentablecolumn:batchUpdate', 'POST', '/generator/column/batchUpdate', 'v1.0.0',
        '字段配置', NULL, 1, 0, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:49', NULL, '2026-02-09 17:03:49',
        NULL);
INSERT INTO forge_admin_new.sys_api_config (id, api_name, api_code, req_method, url_path, api_version, module_code,
                                            service_id, auth_flag, encrypt_flag, tenant_flag, limit_flag,
                                            sensitive_fields, status, remark, create_by, create_time, update_by,
                                            update_time, tenant_id)
VALUES (923, '置顶公告', 'sysnotice:top', 'POST', '/system/notice/top', 'v1.0.0', '通知公告管理', NULL, 0, 1, 1, 0,
        NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:49', NULL, '2026-02-09 17:03:49', NULL),
       (924, 'ExcelExportConfig-remove', 'sysexcelexportconfig:remove', 'DELETE', '/system/excel/export-config/{ids}',
        'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:49', NULL,
        '2026-02-09 17:03:49', NULL),
       (925, 'Sequence-nextBatch', 'sequence:nextBatch', 'GET', '/sequence/nextBatch', 'v1.0.0', 'starter', NULL, 1, 0,
        1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:49', NULL, '2026-02-09 17:03:49', NULL),
       (926, 'Config-edit', 'sysconfig:edit', 'POST', '/system/config/edit', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL,
        1, '系统自动注册', NULL, '2026-02-09 17:03:49', NULL, '2026-02-09 17:03:49', NULL),
       (927, 'ApiConfig-getById', 'sysapiconfig:getById', 'POST', '/system/apiConfig/getById', 'v1.0.0', 'starter',
        NULL, 1, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:49', NULL, '2026-02-09 17:03:49', NULL),
       (928, 'Tenant-removeBatch', 'systenant:removeBatch', 'POST', '/system/tenant/removeBatch', 'v1.0.0', 'plugin',
        NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:49', NULL, '2026-02-09 17:03:49', NULL),
       (929, 'DictData-removeBatch', 'sysdictdata:removeBatch', 'POST', '/system/dict/data/removeBatch', 'v1.0.0',
        'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:50', NULL, '2026-02-09 17:03:50',
        NULL),
       (930, 'DictType-add', 'sysdicttype:add', 'POST', '/system/dict/type/add', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0,
        NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:50', NULL, '2026-02-09 17:03:50', NULL),
       (931, '批量删除缓存', 'syscache:removeBatch', 'POST', '/system/cache/removeBatch', 'v1.0.0', '缓存管理', NULL, 0,
        1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:50', NULL, '2026-02-09 17:03:50', NULL),
       (932, 'User-getUserRoleIds', 'sysuser:getUserRoleIds', 'GET', '/system/user/{userId}/roles', 'v1.0.0', 'plugin',
        NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:50', NULL, '2026-02-09 17:03:50', NULL);
INSERT INTO forge_admin_new.sys_api_config (id, api_name, api_code, req_method, url_path, api_version, module_code,
                                            service_id, auth_flag, encrypt_flag, tenant_flag, limit_flag,
                                            sensitive_fields, status, remark, create_by, create_time, update_by,
                                            update_time, tenant_id)
VALUES (933, '新增数据源', 'gendatasource:add', 'POST', '/generator/datasource/add', 'v1.0.0', '数据源管理', NULL, 1, 0,
        1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:50', NULL, '2026-02-09 17:03:50', NULL),
       (934, 'LoginLog-detail', 'sysloginlog:detail', 'GET', '/system/loginLog/{id}', 'v1.0.0', 'plugin', NULL, 1, 1, 1,
        0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:50', NULL, '2026-02-09 17:03:50', NULL),
       (935, 'Auth-getCaptcha', 'auth:getCaptcha', 'GET', '/auth/captcha', 'v1.0.0', 'starter', NULL, 1, 0, 0, 0, NULL,
        1, '系统自动注册', NULL, '2026-02-09 17:03:50', NULL, '2026-02-09 17:03:50', NULL),
       (936, 'ExcelExportConfig-detail', 'sysexcelexportconfig:detail', 'POST', '/system/excel/export-config/detail',
        'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:50', NULL,
        '2026-02-09 17:03:50', NULL),
       (937, 'ExcelExportConfig-updateStatus', 'sysexcelexportconfig:updateStatus', 'PUT',
        '/system/excel/export-config/status', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL,
        '2026-02-09 17:03:50', NULL, '2026-02-09 17:03:50', NULL),
       (938, 'User-edit', 'sysuser:edit', 'POST', '/system/user/edit', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:50', NULL, '2026-02-09 17:03:50', NULL),
       (939, 'Org-edit', 'sysorg:edit', 'POST', '/system/org/edit', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:50', NULL, '2026-02-09 17:03:50', NULL),
       (940, 'Role-edit', 'sysrole:edit', 'POST', '/system/role/edit', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:50', NULL, '2026-02-09 17:03:50', NULL),
       (941, 'Post-remove', 'syspost:remove', 'POST', '/system/post/remove', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL,
        1, '系统自动注册', NULL, '2026-02-09 17:03:50', NULL, '2026-02-09 17:03:50', NULL),
       (942, 'DataScopeConfig-removeBatch', 'sysdatascopeconfig:removeBatch', 'POST',
        '/system/dataScopeConfig/removeBatch', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL,
        '2026-02-09 17:03:50', NULL, '2026-02-09 17:03:50', NULL);
INSERT INTO forge_admin_new.sys_api_config (id, api_name, api_code, req_method, url_path, api_version, module_code,
                                            service_id, auth_flag, encrypt_flag, tenant_flag, limit_flag,
                                            sensitive_fields, status, remark, create_by, create_time, update_by,
                                            update_time, tenant_id)
VALUES (943, 'Auth-resetPassword', 'auth:resetPassword', 'POST', '/auth/resetPassword', 'v1.0.0', 'starter', NULL, 1, 0,
        1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:50', NULL, '2026-02-09 17:03:50', NULL),
       (944, 'KeyExchange-exchangeKey', 'keyexchange:exchangeKey', 'POST', '/crypto/exchange', 'v1.0.0', 'starter',
        NULL, 0, 0, 0, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:50', NULL, '2026-02-09 17:03:50', NULL),
       (945, '刷新单个API配置缓存', 'apiconfigmanage:refreshSingle', 'POST', '/apiConfig/refreshSingle', 'v1.0.0',
        'API配置管理', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:50', NULL,
        '2026-02-09 17:03:50', NULL),
       (946, 'Role-unbindResources', 'sysrole:unbindResources', 'POST', '/system/role/{roleId}/resources/unbind',
        'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:50', NULL,
        '2026-02-09 17:03:50', NULL),
       (947, 'DictType-page', 'sysdicttype:page', 'GET', '/system/dict/type/page', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0,
        NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:50', NULL, '2026-02-09 17:03:50', NULL),
       (948, '查询缓存详情', 'syscache:getInfo', 'POST', '/system/cache/getInfo', 'v1.0.0', '缓存管理', NULL, 0, 1, 1,
        0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:50', NULL, '2026-02-09 17:03:50', NULL),
       (949, 'Notice-getUserUnreadCount', 'sysnotice:getUserUnreadCount', 'GET', '/system/notice/user/unread-count',
        'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:50', NULL,
        '2026-02-09 17:03:50', NULL),
       (950, 'ExcelExportConfig-page', 'sysexcelexportconfig:page', 'GET', '/system/excel/export-config/page', 'v1.0.0',
        'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:50', NULL, '2026-02-09 17:03:50',
        NULL),
       (951, 'Role-remove', 'sysrole:remove', 'POST', '/system/role/remove', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL,
        1, '系统自动注册', NULL, '2026-02-09 17:03:50', NULL, '2026-02-09 17:03:50', NULL),
       (952, 'DictType-list', 'sysdicttype:list', 'GET', '/system/dict/type/list', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0,
        NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:51', NULL, '2026-02-09 17:03:51', NULL);
INSERT INTO forge_admin_new.sys_api_config (id, api_name, api_code, req_method, url_path, api_version, module_code,
                                            service_id, auth_flag, encrypt_flag, tenant_flag, limit_flag,
                                            sensitive_fields, status, remark, create_by, create_time, update_by,
                                            update_time, tenant_id)
VALUES (953, 'JobConfig-start', 'jobconfig:start', 'POST', '/job/config/{id}/start', 'v1.0.0', 'plugin', NULL, 0, 1, 1,
        0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:51', NULL, '2026-02-09 17:03:51', NULL),
       (954, 'OnlineUser-kickoutUser', 'sysonlineuser:kickoutUser', 'POST', '/auth/online/kickout', 'v1.0.0', 'plugin',
        NULL, 1, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:51', NULL, '2026-02-09 17:03:51', NULL),
       (955, 'Post-edit', 'syspost:edit', 'POST', '/system/post/edit', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:51', NULL, '2026-02-09 17:03:51', NULL),
       (956, 'ExcelExportConfig-add', 'sysexcelexportconfig:add', 'POST', '/system/excel/export-config', 'v1.0.0',
        'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:51', NULL, '2026-02-09 17:03:51',
        NULL),
       (957, 'DictType-edit', 'sysdicttype:edit', 'POST', '/system/dict/type/edit', 'v1.0.0', 'plugin', NULL, 0, 1, 1,
        0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:51', NULL, '2026-02-09 17:03:51', NULL),
       (958, '分页查询缓存列表', 'syscache:page', 'GET', '/system/cache/page', 'v1.0.0', '缓存管理', NULL, 0, 1, 1, 0,
        NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:51', NULL, '2026-02-09 17:03:51', NULL),
       (959, 'GenTemplate-preview', 'gentemplate:preview', 'POST', '/generator/template/preview', 'v1.0.0', 'plugin',
        NULL, 1, 0, 1, 0, NULL, 1, '系统自动注册', NULL, '2026-02-09 17:03:51', NULL, '2026-02-09 17:03:51', NULL),
       (960, 'ExcelColumnConfig-saveBatch', 'sysexcelcolumnconfig:saveBatch', 'POST',
        '/system/excel/column-config/batch', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1, '系统自动注册', NULL,
        '2026-02-09 17:03:51', NULL, '2026-02-09 17:03:51', NULL),
       (961, 'JobConfig-add', 'jobconfig:add', 'POST', '/job/config', 'v1.0.0', 'plugin', NULL, 0, 1, 1, 0, NULL, 1,
        '系统自动注册', NULL, '2026-02-09 17:03:51', NULL, '2026-02-09 17:03:51', NULL),
       (962, 'Tenant-page', 'systenant:page', 'GET', '/system/tenant/page', 'v1.0.0', 'plugin', NULL, 1, 1, 1, 0, NULL,
        1, '系统自动注册', NULL, '2026-02-09 17:03:51', NULL, '2026-02-09 17:04:53', NULL);
INSERT INTO forge_admin_new.sys_config (config_id, tenant_id, config_name, config_key, config_value, config_type,
                                        config_desc, sort, create_by, create_time, update_by, update_time, create_dept)
VALUES (1, 1, '用户初始密码', 'sys.user.initPassword', '123456', 'Y', '用户注册时的默认密码', 1, NULL,
        '2025-11-12 17:41:18', NULL, '2025-12-11 17:21:30', NULL),
       (2, 1, '账号自助注册', 'sys.account.registerUser', 'false', 'Y', '是否开启用户自助注册功能', 2, NULL,
        '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL),
       (4, 1, '用户初始密码1', 'sys.user.initPassword1', '123456', 'Y', '用户注册时的默认密码', 1, NULL,
        '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL);
INSERT INTO forge_admin_new.sys_config_group (id, group_code, group_name, group_icon, config_value, sort, status,
                                              remark, create_time, update_time)
VALUES (2, 'login', '登录配置', 'lock',
        '{"enableCaptcha":true,"captchaType":"slider","enableRememberMe":true,"rememberMeDays":30,"enableLoginLog":true,"enableIpLimit":false,"ipWhitelist":""}',
        2, 1, '登录相关配置', '2026-02-25 18:07:35', '2026-03-02 10:53:22'),
       (4, 'watermark', '水印配置', 'eye',
        '{"enable":true,"content":"name_phone","opacity":0.3,"fontSize":16,"fontColor":"#cccccc","rotate":-20,"gapX":100,"gapY":100,"offsetX":0,"offsetY":0,"showTimestamp":false,"timestampFormat":"yyyy-MM-dd HH:mm:ss","zindex":1000}',
        4, 1, '水印相关配置', '2026-02-25 18:07:35', '2026-03-02 15:05:11'),
       (5, 'crypto', '加解密配置', 'eye',
        '{"enabled":true,"algorithm":"SM4","secretKey":null,"enableDynamicKey":true,"rsaPublicKey":null,"rsaPrivateKey":null,"sessionKeyExpire":7200,"enableApiCrypto":true,"enableFieldCrypto":true,"enableReplayProtection":true,"replayTimeWindow":300,"replayIncludePaths":[],"replayExcludePaths":["/auth/captcha","/crypto/public-key","/ws/**","/api/flow/instance/**","/api/file/**"],"excludePaths":[]}',
        4, 1, '加解密配置', '2026-02-25 18:07:35', '2026-03-28 16:54:04'),
       (6, 'auth', '认证配置', 'eye',
        '{"enableApiPermission":true,"apiPermissionExcludePaths":["/auth/**"],"enableLoginLock":true,"maxLoginAttempts":4,"lockDuration":30,"failRecordExpire":15,"sameAccountLoginStrategy":"replace_old","enableOnlineUserManagement":true}',
        4, 1, '认证配置', '2026-02-25 18:07:35', '2026-02-26 10:04:15'),
       (7, 'log', '日志配置', 'eye',
        '{"enableOperationLog":true,"enableLoginLog":true,"requestParamsMaxLength":2000,"responseResultMaxLength":2000,"excludePaths":["/auth/captcha","/actuator/**","/swagger-ui/**","/v3/api-docs/**"],"printOperationLog":true,"printLoginLog":true,"threadPoolCoreSize":2,"threadPoolMaxSize":5,"threadPoolQueueCapacity":500}',
        4, 1, '日志配置', '2026-02-25 18:07:35', '2026-02-26 12:57:22'),
       (8, 'security', '安全配置', 'eye',
        '{"saToken":{"timeout":2592000,"activityTimeout":-1,"isConcurrent":false,"isShare":false,"isReadBody":true,"isReadHeader":true,"isReadCookie":false,"tokenPrefix":"Bearer","tokenName":"Authorization"},"passwordPolicy":{"minLength":8,"requireUppercase":true,"requireLowercase":true,"requireNumbers":true,"requireSpecialChars":false,"expireDays":90,"historyCount":5}}',
        4, 1, '安全配置', '2026-02-25 18:07:35', '2026-02-26 10:35:14');
INSERT INTO forge_admin_new.sys_data_scope_config (id, tenant_id, resource_code, resource_name, mapper_method,
                                                   table_alias, user_id_column, org_id_column, tenant_id_column,
                                                   enabled, remark, create_by, create_time, update_by, update_time,
                                                   create_dept)
VALUES (1, 1, 'system:user:list', '用户列表查询', 'com.mdframe.forge.plugin.system.mapper.SysUserMapper.selectList', '',
        'id', 'create_dept', 'tenant_id', 1, '用户列表数据权限控制', NULL, '2025-12-03 15:48:28', 1,
        '2026-01-20 15:18:48', NULL),
       (2, 1, 'system:user:query', '用户详情查询', 'com.mdframe.forge.plugin.system.mapper.SysUserMapper.selectById',
        't', 'create_by', 'create_dept', 'tenant_id', 1, '用户详情数据权限控制', NULL, '2025-12-03 15:48:28', 1,
        '2026-01-20 15:18:45', NULL);
INSERT INTO forge_admin_new.sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type,
                                           css_class, list_class, is_default, parent_dict_code, linked_dict_type,
                                           linked_dict_value, dict_status, remark, create_by, create_time, update_by,
                                           update_time, create_dept)
VALUES (1, 1, 1, '未知', '0', 'sys_org_type', '', 'success', 'Y', NULL, NULL, NULL, 1, '性别未知', NULL,
        '2025-11-12 17:41:18', NULL, '2025-12-12 10:50:32', NULL),
       (2, 1, 2, '男', '1', 'sys_user_sex', '', '', 'N', NULL, NULL, NULL, 1, '性别男', NULL, '2025-11-12 17:41:18',
        NULL, '2025-11-12 17:41:18', NULL),
       (3, 1, 3, '女', '2', 'sys_user_sex', '', '', 'N', NULL, NULL, NULL, 1, '性别女', NULL, '2025-11-12 17:41:18',
        NULL, '2025-11-12 17:41:18', NULL),
       (4, 1, 1, '禁用', '0', 'sys_user_status', '', 'danger', 'N', NULL, NULL, NULL, 1, '用户禁用状态', NULL,
        '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL),
       (5, 1, 2, '正常', '1', 'sys_user_status', '', 'success', 'Y', NULL, NULL, NULL, 1, '用户正常状态', NULL,
        '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL),
       (6, 1, 3, '锁定', '2', 'sys_user_status', '', 'warning', 'N', NULL, NULL, NULL, 1, '用户锁定状态', NULL,
        '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL),
       (7, 1, 1, '公司', '1', 'sys_org_type', '', 'info', 'Y', NULL, NULL, NULL, 1, '公司组织', NULL,
        '2025-11-12 17:41:18', NULL, '2025-12-12 10:46:21', NULL),
       (8, 1, 2, '部门', '2', 'sys_org_type', '', 'success', 'N', NULL, NULL, NULL, 1, '部门组织', NULL,
        '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL),
       (9, 1, 3, '小组', '3', 'sys_org_type', '', 'info', 'N', NULL, NULL, NULL, 1, '小组组织', NULL,
        '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL),
       (10, 1, 1, '是', 'Y', 'yes_no', '', '', 'N', NULL, NULL, NULL, 1, '小组组织', NULL, '2025-11-12 17:41:18', NULL,
        '2025-11-12 17:41:18', NULL);
INSERT INTO forge_admin_new.sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type,
                                           css_class, list_class, is_default, parent_dict_code, linked_dict_type,
                                           linked_dict_value, dict_status, remark, create_by, create_time, update_by,
                                           update_time, create_dept)
VALUES (11, 1, 2, '否', 'N', 'yes_no', '', '', 'N', NULL, NULL, NULL, 1, '小组组织', NULL, '2025-11-12 17:41:18', NULL,
        '2025-11-12 17:41:18', NULL),
       (12, 1, 1, '通知公告', 'NOTICE', 'sys_notice_type', NULL, NULL, 'N', NULL, NULL, NULL, 1, '普通通知公告', NULL,
        '2025-12-04 10:57:35', NULL, '2025-12-08 16:31:36', NULL),
       (13, 1, 2, '系统公告', 'ANNOUNCEMENT', 'sys_notice_type', NULL, NULL, 'N', NULL, NULL, NULL, 1, '系统级公告',
        NULL, '2025-12-04 10:57:35', NULL, '2025-12-08 16:31:36', NULL),
       (14, 1, 3, '新闻动态', 'NEWS', 'sys_notice_type', NULL, NULL, 'N', NULL, NULL, NULL, 1, '新闻动态信息', NULL,
        '2025-12-04 10:57:35', NULL, '2025-12-08 16:31:37', NULL),
       (15, 1, 1, '草稿', '0', 'sys_notice_status', NULL, NULL, 'N', NULL, NULL, NULL, 1, '草稿状态', NULL,
        '2025-12-04 10:57:35', NULL, '2025-12-08 16:31:37', NULL),
       (16, 1, 2, '已发布', '1', 'sys_notice_status', NULL, NULL, 'N', NULL, NULL, NULL, 1, '已发布状态', NULL,
        '2025-12-04 10:57:35', NULL, '2025-12-08 16:31:37', NULL),
       (17, 1, 3, '已撤回', '2', 'sys_notice_status', NULL, NULL, 'N', NULL, NULL, NULL, 1, '已撤回状态', NULL,
        '2025-12-04 10:57:35', NULL, '2025-12-08 16:31:37', NULL),
       (18, 1, 1, '待办', '1', 'case_status', NULL, NULL, 'N', NULL, NULL, NULL, 1, '', NULL, '2025-12-05 14:37:28',
        NULL, '2025-12-08 16:31:37', NULL),
       (19, 1, 2, '已办', '0', 'case_status', NULL, NULL, 'N', NULL, NULL, NULL, 1, '', NULL, '2025-12-05 14:37:28',
        NULL, '2025-12-08 16:31:37', NULL),
       (21, 0, 1, 'GET', 'GET', 'req_method', NULL, NULL, 'N', NULL, NULL, NULL, 1, 'GET请求', NULL,
        '2026-02-06 14:59:30', NULL, '2026-02-06 14:59:30', NULL);
INSERT INTO forge_admin_new.sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type,
                                           css_class, list_class, is_default, parent_dict_code, linked_dict_type,
                                           linked_dict_value, dict_status, remark, create_by, create_time, update_by,
                                           update_time, create_dept)
VALUES (22, 0, 2, 'POST', 'POST', 'req_method', NULL, NULL, 'N', NULL, NULL, NULL, 1, 'POST请求', NULL,
        '2026-02-06 14:59:30', NULL, '2026-02-06 14:59:30', NULL),
       (23, 0, 3, 'PUT', 'PUT', 'req_method', NULL, NULL, 'N', NULL, NULL, NULL, 1, 'PUT请求', NULL,
        '2026-02-06 14:59:30', NULL, '2026-02-06 14:59:30', NULL),
       (24, 0, 4, 'DELETE', 'DELETE', 'req_method', NULL, NULL, 'N', NULL, NULL, NULL, 1, 'DELETE请求', NULL,
        '2026-02-06 14:59:30', NULL, '2026-02-06 14:59:30', NULL),
       (25, 0, 5, 'PATCH', 'PATCH', 'req_method', NULL, NULL, 'N', NULL, NULL, NULL, 1, 'PATCH请求', NULL,
        '2026-02-06 14:59:30', NULL, '2026-02-06 14:59:30', NULL),
       (26, 0, 6, 'ALL', 'ALL', 'req_method', NULL, NULL, 'N', NULL, NULL, NULL, 1, '所有请求方式', NULL,
        '2026-02-06 14:59:30', NULL, '2026-02-06 14:59:30', NULL),
       (27, 0, 1, '系统管理', 'sys', 'module_code', NULL, NULL, 'N', NULL, NULL, NULL, 1, '系统管理模块', NULL,
        '2026-02-06 14:59:37', NULL, '2026-02-06 14:59:37', NULL),
       (28, 0, 2, '用户管理', 'user', 'module_code', NULL, NULL, 'N', NULL, NULL, NULL, 1, '用户管理模块', NULL,
        '2026-02-06 14:59:37', NULL, '2026-02-06 14:59:37', NULL),
       (29, 0, 3, '角色管理', 'role', 'module_code', NULL, NULL, 'N', NULL, NULL, NULL, 1, '角色管理模块', NULL,
        '2026-02-06 14:59:37', NULL, '2026-02-06 14:59:37', NULL),
       (30, 0, 4, '菜单管理', 'menu', 'module_code', NULL, NULL, 'N', NULL, NULL, NULL, 1, '菜单管理模块', NULL,
        '2026-02-06 14:59:37', NULL, '2026-02-06 14:59:37', NULL),
       (31, 0, 5, '部门管理', 'dept', 'module_code', NULL, NULL, 'N', NULL, NULL, NULL, 1, '部门管理模块', NULL,
        '2026-02-06 14:59:37', NULL, '2026-02-06 14:59:37', NULL);
INSERT INTO forge_admin_new.sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type,
                                           css_class, list_class, is_default, parent_dict_code, linked_dict_type,
                                           linked_dict_value, dict_status, remark, create_by, create_time, update_by,
                                           update_time, create_dept)
VALUES (32, 0, 6, '岗位管理', 'post', 'module_code', NULL, NULL, 'N', NULL, NULL, NULL, 1, '岗位管理模块', NULL,
        '2026-02-06 14:59:37', NULL, '2026-02-06 14:59:37', NULL),
       (33, 0, 7, '字典管理', 'dict', 'module_code', NULL, NULL, 'N', NULL, NULL, NULL, 1, '字典管理模块', NULL,
        '2026-02-06 14:59:37', NULL, '2026-02-06 14:59:37', NULL),
       (34, 0, 8, '参数管理', 'config', 'module_code', NULL, NULL, 'N', NULL, NULL, NULL, 1, '参数管理模块', NULL,
        '2026-02-06 14:59:37', NULL, '2026-02-06 14:59:37', NULL),
       (35, 0, 9, '通知管理', 'notice', 'module_code', NULL, NULL, 'N', NULL, NULL, NULL, 1, '通知管理模块', NULL,
        '2026-02-06 14:59:37', NULL, '2026-02-06 14:59:37', NULL),
       (36, 0, 10, '日志管理', 'log', 'module_code', NULL, NULL, 'N', NULL, NULL, NULL, 1, '日志管理模块', NULL,
        '2026-02-06 14:59:37', NULL, '2026-02-06 14:59:37', NULL),
       (37, 0, 11, '文件管理', 'file', 'module_code', NULL, NULL, 'N', NULL, NULL, NULL, 1, '文件管理模块', NULL,
        '2026-02-06 14:59:37', NULL, '2026-02-06 14:59:37', NULL),
       (38, 0, 12, '任务管理', 'job', 'module_code', NULL, NULL, 'N', NULL, NULL, NULL, 1, '任务管理模块', NULL,
        '2026-02-06 14:59:37', NULL, '2026-02-06 14:59:37', NULL),
       (39, 0, 13, '数据权限', 'datascope', 'module_code', NULL, NULL, 'N', NULL, NULL, NULL, 1, '数据权限模块', NULL,
        '2026-02-06 14:59:37', NULL, '2026-02-06 14:59:37', NULL),
       (40, 0, 14, 'API配置', 'apiconfig', 'module_code', NULL, NULL, 'N', NULL, NULL, NULL, 1, 'API配置管理模块', NULL,
        '2026-02-06 14:59:37', NULL, '2026-02-06 14:59:37', NULL),
       (41, 1, 0, '图形验证码', 'graphical', 'captcha_type', NULL, 'default', 'N', NULL, NULL, NULL, 1, NULL, 1,
        '2026-02-26 10:13:12', 1, '2026-02-26 10:13:12', 2);
INSERT INTO forge_admin_new.sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type,
                                           css_class, list_class, is_default, parent_dict_code, linked_dict_type,
                                           linked_dict_value, dict_status, remark, create_by, create_time, update_by,
                                           update_time, create_dept)
VALUES (42, 1, 0, '滑块验证码', 'slider', 'captcha_type', NULL, 'default', 'N', NULL, NULL, NULL, 1, NULL, 1,
        '2026-02-26 10:13:28', 1, '2026-02-26 10:13:28', 2),
       (43, 1, 0, '短信验证码', 'sms', 'captcha_type', NULL, 'default', 'N', NULL, NULL, NULL, 1, NULL, 1,
        '2026-02-26 10:13:42', 1, '2026-02-26 10:13:42', 2),
       (44, 1, 0, '姓名+手机号', 'name_phone', 'water_marker_content', NULL, 'default', 'N', NULL, NULL, NULL, 1, NULL,
        1, '2026-02-26 10:19:16', 1, '2026-02-26 10:19:16', 2);
INSERT INTO forge_admin_new.sys_dict_type (dict_id, tenant_id, dict_name, dict_type, dict_status, remark, create_by,
                                           create_time, update_by, update_time, create_dept)
VALUES (1, 1, '用户性别', 'sys_user_sex', 1, '用户性别列表', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18',
        NULL),
       (2, 1, '用户状态', 'sys_user_status', 1, '用户状态列表', NULL, '2025-11-12 17:41:18', NULL,
        '2025-11-12 17:41:18', NULL),
       (3, 1, '组织类型', 'sys_org_type', 1, '组织类型列表', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18',
        NULL),
       (4, 1, '是否', 'yes_no', 1, '是否', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL),
       (5, 0, '公告类型', 'sys_notice_type', 1, '通知公告类型字典', NULL, '2025-12-04 10:57:35', NULL,
        '2025-12-04 10:57:35', NULL),
       (6, 0, '公告发布状态', 'sys_notice_status', 1, '通知公告发布状态字典', NULL, '2025-12-04 10:57:35', NULL,
        '2025-12-04 10:57:35', NULL),
       (7, 0, '案件状态', 'case_status', 1, '案件信息案件状态字典', NULL, '2025-12-05 14:37:28', NULL,
        '2025-12-05 14:37:28', NULL),
       (8, 1, '123123', '123123123123', 1, NULL, NULL, '2025-12-11 16:34:43', NULL, '2025-12-11 16:34:43', NULL),
       (9, 1, '测试', '121', 1, NULL, 1, '2026-01-20 14:34:25', 1, '2026-01-20 14:34:25', 1),
       (10, 0, '请求方式', 'req_method', 1, 'API请求方式字典', NULL, '2026-02-06 14:58:52', NULL, '2026-02-06 14:58:52',
        NULL);
INSERT INTO forge_admin_new.sys_dict_type (dict_id, tenant_id, dict_name, dict_type, dict_status, remark, create_by,
                                           create_time, update_by, update_time, create_dept)
VALUES (11, 0, '业务模块', 'module_code', 1, '业务模块编码字典', NULL, '2026-02-06 14:59:33', NULL,
        '2026-02-06 14:59:33', NULL),
       (12, 1, '验证码类型', 'captcha_type', 1, NULL, 1, '2026-02-26 10:12:04', 1, '2026-02-26 10:12:04', 2),
       (13, 1, '水印内容', 'water_marker_content', 1, NULL, 1, '2026-02-26 10:18:47', 1, '2026-02-26 10:18:47', 2);
INSERT INTO forge_admin_new.sys_excel_column_config (id, config_key, field_name, column_name, width, order_num, export,
                                                     date_format, number_format, dict_type, create_time, update_time,
                                                     create_by, update_by, create_dept)
VALUES (1, 'user_list_export', 'userId', '用户ID', 15, 1, 1, NULL, NULL, NULL, '2025-11-28 15:24:53',
        '2025-11-28 15:24:53', NULL, NULL, NULL),
       (2, 'user_list_export', 'username', '用户名', 20, 2, 1, NULL, NULL, NULL, '2025-11-28 15:24:53',
        '2025-11-28 15:24:53', NULL, NULL, NULL),
       (3, 'user_list_export', 'nickname', '昵称', 20, 3, 1, NULL, NULL, NULL, '2025-11-28 15:24:53',
        '2025-11-28 15:24:53', NULL, NULL, NULL),
       (4, 'user_list_export', 'status', '状态', 15, 4, 1, NULL, NULL, 'user_status', '2025-11-28 15:24:53',
        '2025-11-28 15:24:53', NULL, NULL, NULL),
       (5, 'user_list_export', 'statusName', '状态说明', 15, 5, 1, NULL, NULL, NULL, '2025-11-28 15:24:53',
        '2025-11-28 15:24:53', NULL, NULL, NULL),
       (6, 'user_list_export', 'createTime', '创建时间', 25, 6, 1, NULL, NULL, NULL, '2025-11-28 15:24:53',
        '2025-11-28 15:24:53', NULL, NULL, NULL),
       (7, 'order_list_export', 'orderId', '订单编号', 25, 1, 1, NULL, NULL, NULL, '2025-11-28 15:24:53',
        '2025-11-28 15:24:53', NULL, NULL, NULL),
       (8, 'order_list_export', 'userName', '用户名', 20, 2, 1, NULL, NULL, NULL, '2025-11-28 15:24:53',
        '2025-11-28 15:24:53', NULL, NULL, NULL),
       (9, 'order_list_export', 'totalAmount', '订单金额', 15, 3, 1, NULL, NULL, NULL, '2025-11-28 15:24:53',
        '2025-11-28 15:24:53', NULL, NULL, NULL),
       (10, 'order_list_export', 'orderStatus', '订单状态', 15, 4, 1, NULL, NULL, 'order_status', '2025-11-28 15:24:53',
        '2025-11-28 15:24:53', NULL, NULL, NULL);
INSERT INTO forge_admin_new.sys_excel_column_config (id, config_key, field_name, column_name, width, order_num, export,
                                                     date_format, number_format, dict_type, create_time, update_time,
                                                     create_by, update_by, create_dept)
VALUES (11, 'order_list_export', 'orderStatusName', '状态说明', 15, 5, 1, NULL, NULL, NULL, '2025-11-28 15:24:53',
        '2025-11-28 15:24:53', NULL, NULL, NULL),
       (12, 'order_list_export', 'createTime', '下单时间', 25, 6, 1, NULL, NULL, NULL, '2025-11-28 15:24:53',
        '2025-11-28 15:24:53', NULL, NULL, NULL),
       (13, 'test_export', 'configName', '参数名称', 25, 6, 1, NULL, NULL, NULL, '2025-11-28 15:24:53',
        '2025-11-28 15:24:53', NULL, NULL, NULL),
       (14, 'test_export', 'configTypeName', '系统内置', 25, 6, 1, NULL, NULL, NULL, '2025-11-28 15:24:53',
        '2025-11-28 15:24:53', NULL, NULL, NULL),
       (15, 'system_legalCase_export', 'caseName', '案件名称', 20, 2, 1, NULL, NULL, NULL, '2026-01-21 22:35:26',
        '2026-01-21 22:35:26', 1, 1, 2),
       (16, 'system_legalCase_export', 'caseStatus', '案件状态(1:待办,0:已办)', 20, 1, 1, NULL, NULL, 'case_status',
        '2026-01-21 22:35:26', '2026-01-21 22:35:26', 1, 1, 2);
INSERT INTO forge_admin_new.sys_excel_export_config (id, config_key, export_name, sheet_name, file_name_template,
                                                     data_source_bean, query_method, auto_trans, pageable, max_rows,
                                                     sort_field, sort_order, status, remark, create_time, update_time,
                                                     create_by, update_by, create_dept)
VALUES (1, 'user_list_export', '用户列表导出', '用户数据', '用户列表_{date}.xlsx', 'sysUserService', 'list', 1, 0,
        50000, NULL, NULL, 1, NULL, '2025-11-28 15:24:53', '2025-11-28 15:24:53', NULL, NULL, NULL),
       (2, 'order_list_export', '订单列表导出', '订单数据', '订单列表_{date}_{time}.xlsx', 'orderService', 'queryList',
        1, 1, 100000, NULL, NULL, 1, NULL, '2025-11-28 15:24:53', '2025-11-28 15:24:53', NULL, NULL, NULL),
       (3, 'test_export', '测试列表导出', '测试数据', '订单列表_{date}_{time}.xlsx', 'sysConfigServiceImpl',
        'selectConfigList', 0, 0, 100000, NULL, NULL, 1, NULL, '2025-11-28 15:24:53', '2025-11-28 15:24:53', NULL, NULL,
        NULL),
       (4, 'system_legalCase_export', '案件信息导出', '案件信息数据', '案件信息列表_{date}.xlsx', 'legalCaseService',
        'selectLegalCaseList', 1, 0, 50000, NULL, NULL, 1, '案件信息数据导出配置', '2025-12-05 14:37:04',
        '2025-12-05 14:37:04', NULL, NULL, NULL),
       (2013984223186178050, 'test11', '1212', '1', '1', '1', '1', 1, 0, 10000, NULL, NULL, 0, NULL,
        '2026-01-21 22:37:11', '2026-01-21 22:55:31', 1, 1, 2);
INSERT INTO forge_admin_new.sys_file_group (id, group_name, group_code, group_type, parent_id, sort, icon, description,
                                            status, create_time, update_time, create_by, update_by, create_dept,
                                            deleted)
VALUES (2028326543566475266, '测试', 'group_1772425707002', 'default', 0, 0, NULL, NULL, 1, '2026-03-02 12:28:27',
        '2026-03-02 12:28:27', 1, 1, 2, 0);
INSERT INTO forge_admin_new.sys_file_metadata (id, file_id, group_id, original_name, storage_name, file_path, file_size,
                                               mime_type, extension, md5, storage_type, bucket, access_url,
                                               thumbnail_url, business_type, business_id, uploader_id, upload_time,
                                               expire_time, is_private, download_count, status, create_time,
                                               update_time, create_by, update_by, create_dept)
VALUES (1995383607610363905, '894a3029cb8a4a68ac49909604b82e76', NULL, '订单列表_20251128_160153.xlsx',
        '799fd49d92a1492eae1c781f953d0a59.xlsx', 'test/2025/12/01/799fd49d92a1492eae1c781f953d0a59.xlsx', 8805,
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 'xlsx', 'ce59f3c2161d26c1e21ba348e44367d8',
        'local', NULL, NULL, NULL, 'test', '12313', NULL, '2025-12-01 14:44:59', NULL, 0, 3, 1, '2025-12-01 14:44:59',
        '2026-01-21 16:52:27', NULL, NULL, NULL),
       (1998573617505304578, '7038e1f773974ecb8b4df2c1305f8c66', NULL, '加载失败.png',
        'a111492084ab4a5fb3f149f76b5f8c4c.png', 'tenant-logo/2025/12/10/a111492084ab4a5fb3f149f76b5f8c4c.png', 10982,
        'image/png', 'png', '1019f22d9f7d142b3b55779a2670958b', 'local', NULL, NULL, NULL, 'tenant-logo', '', NULL,
        '2025-12-10 10:00:57', NULL, 0, 48, 1, '2025-12-10 10:00:56', '2025-12-22 11:27:05', NULL, NULL, NULL),
       (1998574675875004418, '9742f806833b4766aa1e6b5d0bb9cc77', NULL, 'shuangyinhao-left.png',
        '6eb0ab691c3545c7a86ef864c0d06265.png', 'tenant-logo/2025/12/10/6eb0ab691c3545c7a86ef864c0d06265.png', 2301,
        'image/png', 'png', '1d54939820770ee76fbfbaf799e7b43a', 'local', NULL, NULL, NULL, 'tenant-logo', '', NULL,
        '2025-12-10 10:05:09', NULL, 0, 0, 1, '2025-12-10 10:05:09', '2025-12-10 10:05:09', NULL, NULL, NULL),
       (1998575436570755073, 'bd6f3afbbaa747c4abac5aea919903a9', NULL, 'WX20251205-172243@2x.png',
        'e7a3a608d16a47d9a68c5e48f2e1d008.png', 'tenant-logo/2025/12/10/e7a3a608d16a47d9a68c5e48f2e1d008.png', 127920,
        'image/png', 'png', '46459966fb0ff4b140fe563312a4cc37', 'local', NULL, NULL, NULL, 'tenant-logo', '', NULL,
        '2025-12-10 10:08:10', NULL, 0, 8, 1, '2025-12-10 10:08:10', '2025-12-15 15:32:32', NULL, NULL, NULL),
       (1998580016318709761, 'd032857c6a5b4c9cb53a7f1cf48c3f03', NULL, '授权失败.png',
        '3c9d3da67951469bb48fa827fa4a63bb.png', 'tenant-logo/2025/12/10/3c9d3da67951469bb48fa827fa4a63bb.png', 10390,
        'image/png', 'png', '1635f617779fc0bde2a4bb511ad10d3a', 'local', NULL, NULL, NULL, 'tenant-logo', '', NULL,
        '2025-12-10 10:26:22', NULL, 0, 20, 1, '2025-12-10 10:26:22', '2025-12-15 15:49:41', NULL, NULL, NULL),
       (1998583764134854657, '50adbeedf9a24f26af8b25ba2c92e875', NULL, '404.png',
        '64328e986faa4d14bf00fd374dbd009a.png', 'tenant-logo/2025/12/10/64328e986faa4d14bf00fd374dbd009a.png', 44997,
        'image/png', 'png', '5d015704860486d2a4511f5823a598b1', 'local', NULL, NULL, NULL, 'tenant-logo', '', NULL,
        '2025-12-10 10:41:16', NULL, 0, 0, 1, '2025-12-10 10:41:16', '2025-12-10 10:41:16', NULL, NULL, NULL),
       (1998684468870590465, 'e0a0ada8fb534f5a8ed968dac8dc4d16', NULL, 'WechatIMG2751.jpg',
        'fcd8f2cacc59440fb2f95ac0b644fd77.jpg', 'tenant-logo/2025/12/10/fcd8f2cacc59440fb2f95ac0b644fd77.jpg', 168006,
        'image/jpeg', 'jpg', '5abdfe8c2dd523a7b5d9adff29841307', 'local', NULL, NULL, NULL, 'tenant-logo', '', NULL,
        '2025-12-10 17:21:26', NULL, 0, 4, 1, '2025-12-10 17:21:25', '2025-12-15 15:17:24', NULL, NULL, NULL),
       (2000459995732258818, 'bb827371e38243b7bd93aed3fb3b3eaf', NULL, '20250915155404.jpg',
        '782172a7247048ec8eb6b8ecbe17784b.jpg', 'tenant-logo/2025/12/15/782172a7247048ec8eb6b8ecbe17784b.jpg', 25510,
        'image/jpeg', 'jpg', '5295bc1edadd7a5cc71e75e06b1e1fe9', 'local', NULL, NULL, NULL, 'tenant-logo', '', NULL,
        '2025-12-15 14:56:44', NULL, 0, 10, 1, '2025-12-15 14:56:44', '2025-12-15 15:42:47', NULL, NULL, NULL),
       (2000461309581209602, '98b6c6e5800942838017377c01e0cd3a', NULL, '箭头.png',
        '57e6ebfc5ab14ed1b75d58984d3e78f4.png', 'tenant-logo/2025/12/15/57e6ebfc5ab14ed1b75d58984d3e78f4.png', 1174,
        'image/png', 'png', '9da239f7e939e80d84937c8aa929d84e', 'local', NULL, NULL, NULL, 'tenant-logo', '', NULL,
        '2025-12-15 15:01:58', NULL, 0, 1, 1, '2025-12-15 15:01:58', '2025-12-15 15:01:58', NULL, NULL, NULL),
       (2000461328061313025, 'be2b996b38124fa49836b613da3e3275', NULL, 'WX20250211-100736@2x.png',
        'ad7d959c40ff434c9a9c3efcf40be1cf.png', 'tenant-logo/2025/12/15/ad7d959c40ff434c9a9c3efcf40be1cf.png', 18398,
        'image/png', 'png', '9043ab2fa05297924ff5179b774e6f3c', 'local', NULL, NULL, NULL, 'tenant-logo', '', NULL,
        '2025-12-15 15:02:02', NULL, 0, 1, 1, '2025-12-15 15:02:02', '2025-12-15 15:02:02', NULL, NULL, NULL);
INSERT INTO forge_admin_new.sys_file_metadata (id, file_id, group_id, original_name, storage_name, file_path, file_size,
                                               mime_type, extension, md5, storage_type, bucket, access_url,
                                               thumbnail_url, business_type, business_id, uploader_id, upload_time,
                                               expire_time, is_private, download_count, status, create_time,
                                               update_time, create_by, update_by, create_dept)
VALUES (2000461876865019906, '1c509478280c4235afea45cd844393d1', NULL, '封面4.png',
        'b3ba5074969a402181b1c3b959d96c63.png', 'tenant-logo/2025/12/15/b3ba5074969a402181b1c3b959d96c63.png', 952400,
        'image/png', 'png', 'c89176b5c887c7f27f5bec84b0ca1230', 'local', NULL, NULL, NULL, 'tenant-logo', '', NULL,
        '2025-12-15 15:04:13', NULL, 0, 1, 1, '2025-12-15 15:04:13', '2025-12-15 15:04:13', NULL, NULL, NULL),
       (2000462497668149250, '59fff30d10fa4d3d9d40385b5c1ec881', NULL, '小区.png',
        '12d834608a7243a89317d980f9d198c4.png', 'tenant-logo/2025/12/15/12d834608a7243a89317d980f9d198c4.png', 2631,
        'image/png', 'png', 'fc68f176ee1dff005097846a206f5c10', 'local', NULL, NULL, NULL, 'tenant-logo', '', NULL,
        '2025-12-15 15:06:41', NULL, 0, 1, 1, '2025-12-15 15:06:41', '2025-12-15 15:06:41', NULL, NULL, NULL),
       (2013872365678669825, '87eb1f5068934e64afdd5366cf199a02', NULL, 'AGENTS.md',
        'e560acb3794c431b90b9d63554a5fc14.md', 'notice/2026/01/21/e560acb3794c431b90b9d63554a5fc14.md', 7404,
        'application/octet-stream', 'md', '4afedec3116c0b200dc32ddfb56e1242', 'local', NULL, NULL, NULL, 'notice', '',
        NULL, '2026-01-21 15:12:43', NULL, 0, 0, 0, '2026-01-21 15:12:43', '2026-01-21 16:47:21', 1, 1, 1),
       (2013898112174702593, 'b061a31437704322b73fd8082013ba2d', NULL, 'aaa.png',
        '1447d4eeb81841799562580e25191c2e.png', 'common/2026/01/21/1447d4eeb81841799562580e25191c2e.png', 33602,
        'image/png', 'png', '164f6b490c03dd0de816275996c2640a', 'local', NULL, NULL, NULL, 'common', '', NULL,
        '2026-01-21 16:55:01', NULL, 0, 5, 1, '2026-01-21 16:55:01', '2026-03-29 09:32:58', 1, 1, 1),
       (2018867842172018690, 'dab7a91179814cd4aecab3064485be30', NULL, '测试2.png',
        'aa363ba19e4f46709e8a793ff025e273.png', 'notice/2026/02/04/aa363ba19e4f46709e8a793ff025e273.png', 74068,
        'image/png', 'png', 'bddaaadbac2a128dde41275556e71e73', 'local', NULL, NULL, NULL, 'notice', '', NULL,
        '2026-02-04 10:02:57', NULL, 0, 0, 1, '2026-02-04 10:02:57', '2026-02-04 10:02:57', 1, 1, 2),
       (2018875660086239233, '06dbb2a0ef77405bbcad2d60fb1d1ad3', 2028326543566475266, 'ceshi.png',
        'd9eb2d3d1dea40d69d125a7086488226.png', 'notice/2026/02/04/d9eb2d3d1dea40d69d125a7086488226.png', 227550,
        'image/png', 'png', 'fe0ad5e51ad816ad3ba06597fb84c4f8', 'local', NULL, NULL, NULL, 'notice', '', NULL,
        '2026-02-04 10:34:01', NULL, 0, 0, 1, '2026-02-04 10:34:01', '2026-03-02 12:28:38', 1, 1, 2),
       (2037815561079451650, 'd80d8edb35b64c8d9e8aa4a9b8db3742', NULL, '6cd18c655f7b34c5acea68e00c7169b0.jpg',
        '1d6387fb548d4457a755c8fb45746680.jpg', 'common/2026/03/28/1d6387fb548d4457a755c8fb45746680.jpg', 3229654,
        'image/jpeg', 'jpg', '7cf0ae993d5126fc12bd38d54d72290d', 'local', NULL, NULL, NULL, 'common', '', NULL,
        '2026-03-28 16:54:25', NULL, 0, 7, 0, '2026-03-28 16:54:25', '2026-03-29 09:00:26', 1, 1, 2),
       (2038058688415440897, '452e56168abd42a489da1f37ddc91f51', NULL, '6cd18c655f7b34c5acea68e00c7169b0.jpg',
        '5ecd44513c414970954bbf65c0bf7e81.jpg', 'common/2026/03/29/5ecd44513c414970954bbf65c0bf7e81.jpg', 3229654,
        'image/jpeg', 'jpg', '7cf0ae993d5126fc12bd38d54d72290d', 'local', NULL, NULL, NULL, 'common', '', NULL,
        '2026-03-29 09:00:31', NULL, 0, 2, 0, '2026-03-29 09:00:31', '2026-03-29 09:33:31', 1, 1, 2),
       (2038075374384357377, '037456d8c761445191e124dc169f2563', NULL, '6cd18c655f7b34c5acea68e00c7169b0.jpg',
        '503247ed57eb49d2ac7cec1b71161cea.jpg', 'common/2026/03/29/503247ed57eb49d2ac7cec1b71161cea.jpg', 3229654,
        'image/jpeg', 'jpg', '7cf0ae993d5126fc12bd38d54d72290d', 'local', NULL, NULL, NULL, 'common', '', NULL,
        '2026-03-29 10:06:50', NULL, 0, 0, 0, '2026-03-29 10:06:50', '2026-03-29 10:06:54', 1, 1, 2),
       (2038076910762115074, 'fc31f00d8ad147e2a0321095081dbe2f', NULL, '6cd18c655f7b34c5acea68e00c7169b0.jpg',
        'common/2026/03/29/796712ba46b145b1877f8d0ac43eb79b.jpg',
        'common/2026/03/29/796712ba46b145b1877f8d0ac43eb79b.jpg', 3229654, 'image/jpeg', 'jpg',
        '7cf0ae993d5126fc12bd38d54d72290d', 'rustfs', 'forge-files', NULL, NULL, 'common', '', NULL,
        '2026-03-29 10:12:56', NULL, 0, 6, 1, '2026-03-29 10:12:56', '2026-03-29 19:10:57', 1, 1, 2);
INSERT INTO forge_admin_new.sys_file_storage_config (id, config_name, storage_type, is_default, enabled, endpoint,
                                                     access_key, secret_key, bucket_name, region, base_path, `domain`,
                                                     use_https, max_file_size, allowed_types, order_num, extra_config,
                                                     create_time, update_time, create_by, update_by, create_dept)
VALUES (1, '本地存储', 'local', 1, 1, NULL, NULL, NULL, NULL, NULL, '/var/tmp/', NULL, 1, 100,
        'jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,zip,rar,md', 1, NULL, '2025-11-28 16:44:18', '2026-03-29 19:26:50',
        NULL, 1, NULL),
       (3, 'RustFS存储', 'rustfs', 0, 1, 'http://127.0.0.1:9000', 'rustfsadmin', 'rustfsadmin', 'forge-files', NULL,
        NULL, NULL, 0, 500, 'jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,zip,rar,mp4,mp3', 3, NULL, '2026-03-28 16:25:28',
        '2026-03-29 08:56:07', NULL, 1, NULL);
INSERT INTO forge_admin_new.sys_flow_business (id, business_key, business_type, process_instance_id, process_def_id,
                                               process_def_key, title, status, apply_user_id, apply_user_name,
                                               apply_dept_id, apply_dept_name, apply_time, end_time, duration,
                                               create_time, update_time)
VALUES ('0d3d53c3c1429a488889215722b064e9', '34232132131', 'todo_flow', 'd092ca7b-24fe-11f1-9590-c68306b388f0',
        'todo_flow:3:6dccf7a9-24f6-11f1-b21b-c68306b388f0', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', '研发部', '2026-03-21 16:20:27', NULL, NULL, '2026-03-21 16:20:27', '2026-03-21 16:20:27'),
       ('16ce785b5f039dcd2965973ba17177c5', '6666767', 'todo_flow', '81c54e55-24e8-11f1-aa0e-c68306b388f0',
        'todo_flow:1:4c4d01de-2119-11f1-80d9-b69ad982d03a', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', NULL, '2026-03-21 13:40:46', NULL, NULL, '2026-03-21 13:40:46', '2026-03-21 13:40:46'),
       ('17e98ebc181e288631150b3c99c3ea3c', 'LEAVE_B6EC0266969B4DAA', 'leave', '44fd4163-2901-11f1-9444-16f80eb69e69',
        'leave_process:10:08700850-28fe-11f1-9674-e6e756836190', 'leave_process', 'admin 的请假申请', 'approved', '1',
        '超级管理员', '2', '研发部', '2026-03-26 18:48:05', '2026-03-26 18:49:07', 61767, '2026-03-26 18:48:05',
        '2026-03-26 18:48:05'),
       ('2a4d038ea7c2717cacb569b922a7854f', '1231321', 'todo_flow', 'cc59d769-24e0-11f1-9aa5-c68306b388f0',
        'todo_flow:1:4c4d01de-2119-11f1-80d9-b69ad982d03a', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', NULL, '2026-03-21 12:45:35', NULL, NULL, '2026-03-21 12:45:35', '2026-03-21 12:45:35'),
       ('33cbab9a661c4a8d2e0ae83c92f837a1', '111111112', 'todo_flow', '4279da9d-24f7-11f1-b04e-c68306b388f0',
        'todo_flow:3:6dccf7a9-24f6-11f1-b21b-c68306b388f0', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', NULL, '2026-03-21 15:26:22', NULL, NULL, '2026-03-21 15:26:22', '2026-03-21 15:26:22'),
       ('36351e2b4e099a077cb930c37e93eb88', '121212121', 'todo_flow', '384f1154-2516-11f1-b539-c68306b388f0',
        'todo_flow:6:079e9e98-2516-11f1-a15c-c68306b388f0', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', '研发部', '2026-03-21 19:07:59', NULL, NULL, '2026-03-21 19:07:59', '2026-03-21 19:07:59'),
       ('3820414105bb49af01e20ff0aa26063a', 'LEAVE_7EC50E86F49B4A09', 'leave', '5ccfc6c6-2902-11f1-a6c9-16f80eb69e69',
        'leave_process:10:08700850-28fe-11f1-9674-e6e756836190', 'leave_process', 'admin 的请假申请', 'approved', '1',
        '超级管理员', '2', '研发部', '2026-03-26 18:55:55', '2026-03-26 18:56:09', 13990, '2026-03-26 18:55:55',
        '2026-03-26 18:55:55'),
       ('433d7f4ac055f2f9fa7fabc5a683fad5', '9999', 'todo_flow', 'f1517a16-24e7-11f1-98d7-c68306b388f0',
        'todo_flow:1:4c4d01de-2119-11f1-80d9-b69ad982d03a', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', NULL, '2026-03-21 13:36:43', NULL, NULL, '2026-03-21 13:36:43', '2026-03-21 13:36:43'),
       ('455cfc4987087e2551c201e68e9d2d3c', '1213123123123', 'todo_flow', '58b694d1-252e-11f1-b539-c68306b388f0',
        'todo_flow:34:42bb9900-252e-11f1-b539-c68306b388f0', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', '研发部', '2026-03-21 22:00:41', NULL, NULL, '2026-03-21 22:00:41', '2026-03-21 22:00:41'),
       ('50b523a61298e40928a40a720910fbb7', 'LEAVE_9833FDF482904B01', 'leave', '55bfce06-2903-11f1-a6c9-16f80eb69e69',
        'leave_process:10:08700850-28fe-11f1-9674-e6e756836190', 'leave_process', 'admin 的请假申请', 'approved', '1',
        '超级管理员', '2', '研发部', '2026-03-26 19:02:52', '2026-03-26 19:03:04', 12352, '2026-03-26 19:02:52',
        '2026-03-26 19:02:52');
INSERT INTO forge_admin_new.sys_flow_business (id, business_key, business_type, process_instance_id, process_def_id,
                                               process_def_key, title, status, apply_user_id, apply_user_name,
                                               apply_dept_id, apply_dept_name, apply_time, end_time, duration,
                                               create_time, update_time)
VALUES ('58d9750786edf2f2437b5aa29b0a8ae6', '11212121', 'todo_flow', '4e074215-24f2-11f1-9a51-c68306b388f0',
        'todo_flow:2:f07729f9-24e8-11f1-870a-c68306b388f0', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', NULL, '2026-03-21 14:50:54', NULL, NULL, '2026-03-21 14:50:54', '2026-03-21 14:50:54'),
       ('6319b90709b0576c77cdc570016d6a5b', '12321312312312321', 'todo_flow', '97752e96-24f6-11f1-85ed-c68306b388f0',
        'todo_flow:3:6dccf7a9-24f6-11f1-b21b-c68306b388f0', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', NULL, '2026-03-21 15:21:35', NULL, NULL, '2026-03-21 15:21:35', '2026-03-21 15:21:35'),
       ('661994c312949e301f21e9225e35b960', 'hhh12313', 'todo_flow', 'fcd0f320-24f7-11f1-828d-c68306b388f0',
        'todo_flow:3:6dccf7a9-24f6-11f1-b21b-c68306b388f0', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', NULL, '2026-03-21 15:31:35', NULL, NULL, '2026-03-21 15:31:35', '2026-03-21 15:31:35'),
       ('6639876d01d30132a5eaa58f263914db', 'LEAVE_8E319D3F139243D6', 'leave', 'c2e261ed-2904-11f1-b7a8-16f80eb69e69',
        'leave_process:10:08700850-28fe-11f1-9674-e6e756836190', 'leave_process', 'admin 的请假申请', 'approved', '1',
        '超级管理员', '2', '研发部', '2026-03-26 19:13:05', '2026-03-26 19:13:17', 12435, '2026-03-26 19:13:05',
        '2026-03-26 19:13:05'),
       ('6a04fe90d4d6defd5c937aaed98d71fb', '12121212', 'todo_flow', 'faef3734-24f3-11f1-8215-c68306b388f0',
        'todo_flow:2:f07729f9-24e8-11f1-870a-c68306b388f0', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', NULL, '2026-03-21 15:02:54', NULL, NULL, '2026-03-21 15:02:54', '2026-03-21 15:02:54'),
       ('6ca5ba085fc63773d9b34ff0e54d5f46', '7777', 'todo_flow', '5a18dc50-24e7-11f1-98d7-c68306b388f0',
        'todo_flow:1:4c4d01de-2119-11f1-80d9-b69ad982d03a', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', NULL, '2026-03-21 13:32:30', NULL, NULL, '2026-03-21 13:32:30', '2026-03-21 13:32:30'),
       ('6e3b60716f613b2e197d2814230bb97c', 'LEAVE_84CB073379C3414D', 'leave', '2144b101-28fe-11f1-9674-e6e756836190',
        'leave_process:10:08700850-28fe-11f1-9674-e6e756836190', 'leave_process', 'admin 的请假申请', 'approved', '1',
        '超级管理员', '2', '研发部', '2026-03-26 18:25:37', '2026-03-26 18:37:04', 687340, '2026-03-26 18:25:37',
        '2026-03-26 18:25:37'),
       ('7065183acfc2c60ebb4957519003bef7', 'LEAVE_AD395CD3AF304AD1', 'leave', 'dad177a6-2902-11f1-a6c9-16f80eb69e69',
        'leave_process:10:08700850-28fe-11f1-9674-e6e756836190', 'leave_process', 'admin 的请假申请', 'approved', '1',
        '超级管理员', '2', '研发部', '2026-03-26 18:59:26', '2026-03-26 18:59:39', 12865, '2026-03-26 18:59:26',
        '2026-03-26 18:59:26'),
       ('70e13ba7636a4238fd26890391505c67', 'LEAVE_9BA9DECBFDB34DED', 'leave', 'cb32264b-2906-11f1-b933-16f80eb69e69',
        'leave_process:10:08700850-28fe-11f1-9674-e6e756836190', 'leave_process', 'admin 的请假申请', 'approved', '1',
        '超级管理员', '2', '研发部', '2026-03-26 19:27:38', '2026-03-26 19:29:20', 102002, '2026-03-26 19:27:38',
        '2026-03-26 19:27:38'),
       ('77405c3cd1770cf36bdc944193289c97', '111222', 'todo_flow', '03235aea-24e7-11f1-98d7-c68306b388f0',
        'todo_flow:1:4c4d01de-2119-11f1-80d9-b69ad982d03a', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', NULL, '2026-03-21 13:30:04', NULL, NULL, '2026-03-21 13:30:04', '2026-03-21 13:30:04');
INSERT INTO forge_admin_new.sys_flow_business (id, business_key, business_type, process_instance_id, process_def_id,
                                               process_def_key, title, status, apply_user_id, apply_user_name,
                                               apply_dept_id, apply_dept_name, apply_time, end_time, duration,
                                               create_time, update_time)
VALUES ('7a73fa38227bc712393c0b608a2eea20', 'LEAVE_F752BF1F5D234E34', 'leave', 'cd7973ce-2905-11f1-a323-16f80eb69e69',
        'leave_process:10:08700850-28fe-11f1-9674-e6e756836190', 'leave_process', 'admin 的请假申请', 'approved', '1',
        '超级管理员', '2', '研发部', '2026-03-26 19:20:32', '2026-03-26 19:23:55', 202875, '2026-03-26 19:20:32',
        '2026-03-26 19:20:32'),
       ('7dab5f277675b3fa85231447cdc951e4', 'LEAVE_C9B9D2736FE14D0C', 'leave', 'f9112374-25ee-11f1-a191-c68306b388f0',
        'leave_process:2:8cbcd859-25e9-11f1-90d0-c68306b388f0', 'leave_process', '年假申请 - admin', 'running', '1',
        'admin', NULL, NULL, '2026-03-22 20:59:33', NULL, NULL, '2026-03-22 20:59:33', '2026-03-22 20:59:33'),
       ('90596d537711b26335e3d6967fa4dbff', 'LEAVE_1CB98984B0B542AB', 'leave', 'cee537c7-2910-11f1-88d0-16f80eb69e69',
        'leave_process:10:08700850-28fe-11f1-9674-e6e756836190', 'leave_process', 'admin 的请假申请', 'running', '1',
        '超级管理员', '2', '研发部', '2026-03-26 20:39:19', NULL, NULL, '2026-03-26 20:39:19', '2026-03-26 20:39:19'),
       ('9348aa43b1f7c8b7c4c76183f0f227c9', '1111', 'todo_flow', '55136be2-24e6-11f1-9c9f-c68306b388f0',
        'todo_flow:1:4c4d01de-2119-11f1-80d9-b69ad982d03a', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', NULL, '2026-03-21 13:25:12', NULL, NULL, '2026-03-21 13:25:12', '2026-03-21 13:25:12'),
       ('9590dc0e307d13e928c4d39f058806c0', '111', 'todo_flow', '99fecdce-2437-11f1-87d2-b69ad982d03a',
        'todo_flow:1:4c4d01de-2119-11f1-80d9-b69ad982d03a', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', NULL, '2026-03-20 16:34:26', NULL, NULL, '2026-03-20 16:34:26', '2026-03-20 16:34:26'),
       ('9a838cef01a9f733cb8f535209acca49', 'LEAVE_C055B79F68ED4079', 'leave', '861f7966-2909-11f1-9585-16f80eb69e69',
        'leave_process:10:08700850-28fe-11f1-9674-e6e756836190', 'leave_process', 'admin 的请假申请', 'running', '1',
        '超级管理员', '2', '研发部', '2026-03-26 19:47:10', NULL, NULL, '2026-03-26 19:47:10', '2026-03-26 19:47:10'),
       ('a15ee68d36d2a17f915debfbb0a1b268', '989897', 'todo_flow', '145487aa-24e9-11f1-870a-c68306b388f0',
        'todo_flow:2:f07729f9-24e8-11f1-870a-c68306b388f0', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', NULL, '2026-03-21 13:44:52', NULL, NULL, '2026-03-21 13:44:52', '2026-03-21 13:44:52'),
       ('abf781c245e8ecec7aaa4228c7b5ffe1', 'LEAVE_83FB20E0F70E4ACD', 'leave', '077c0183-28f2-11f1-8984-e6e756836190',
        'leave_process:7:edaf9ac8-28eb-11f1-810f-e6e756836190', 'leave_process', 'admin 的请假申请', 'approved', '1',
        '超级管理员', '2', '研发部', '2026-03-26 16:58:59', '2026-03-26 17:16:30', 1051081, '2026-03-26 16:58:59',
        '2026-03-26 16:58:59'),
       ('ae72f5780d03c1fce29f664112e86a00', 'LEAVE_5835B6CF47C547D7', 'leave', '31b7ec44-28fd-11f1-9674-e6e756836190',
        'leave_process:9:2a237673-28fd-11f1-9674-e6e756836190', 'leave_process', 'admin 的请假申请', 'running', '1',
        '超级管理员', '2', '研发部', '2026-03-26 18:18:55', NULL, NULL, '2026-03-26 18:18:55', '2026-03-26 18:18:55'),
       ('ce92043e4e2cdfa50f234dd1a53515a3', 'LEAVE_71D5E9DF8ACD47CF', 'leave', 'c5d263f7-2909-11f1-950e-16f80eb69e69',
        'leave_process:10:08700850-28fe-11f1-9674-e6e756836190', 'leave_process', 'admin 的请假申请', 'approved', '1',
        '超级管理员', '2', '研发部', '2026-03-26 19:48:57', '2026-03-26 19:50:01', 64199, '2026-03-26 19:48:57',
        '2026-03-26 19:48:57');
INSERT INTO forge_admin_new.sys_flow_business (id, business_key, business_type, process_instance_id, process_def_id,
                                               process_def_key, title, status, apply_user_id, apply_user_name,
                                               apply_dept_id, apply_dept_name, apply_time, end_time, duration,
                                               create_time, update_time)
VALUES ('d8659d4ca4e7704e27a338ced3184ee8', 'hjkhsahdak', 'todo_flow', 'a249f101-24f8-11f1-90cd-c68306b388f0',
        'todo_flow:3:6dccf7a9-24f6-11f1-b21b-c68306b388f0', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', NULL, '2026-03-21 15:36:12', NULL, NULL, '2026-03-21 15:36:12', '2026-03-21 15:36:12'),
       ('d8afe6519f86357cdb8f606c33c1cd58', 'LEAVE_76B0FB0F24504EDD', 'leave', '18949516-2902-11f1-a6c9-16f80eb69e69',
        'leave_process:10:08700850-28fe-11f1-9674-e6e756836190', 'leave_process', 'admin 的请假申请', 'approved', '1',
        '超级管理员', '2', '研发部', '2026-03-26 18:54:00', '2026-03-26 18:54:15', 14693, '2026-03-26 18:54:00',
        '2026-03-26 18:54:00'),
       ('db1a0938ad20c512831afe63d2864d3a', '1231312', 'todo_flow', '35cb69e1-250a-11f1-b85c-c68306b388f0',
        'todo_flow:3:6dccf7a9-24f6-11f1-b21b-c68306b388f0', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', '研发部', '2026-03-21 17:42:01', NULL, NULL, '2026-03-21 17:42:01', '2026-03-21 17:42:01'),
       ('e6a0e67b61363cf6f2184afd27dd42f7', 'LEAVE_3804972B33BE410F', 'leave', 'a362c0b3-2901-11f1-9444-16f80eb69e69',
        'leave_process:10:08700850-28fe-11f1-9674-e6e756836190', 'leave_process', 'admin 的请假申请', 'approved', '1',
        '超级管理员', '2', '研发部', '2026-03-26 18:50:44', '2026-03-26 18:50:55', 10930, '2026-03-26 18:50:44',
        '2026-03-26 18:50:44');
INSERT INTO forge_admin_new.sys_flow_category (id, category_code, category_name, description, sort_order, status,
                                               create_time, update_time)
VALUES ('1', 'leave', '请假流程', '员工请假申请审批', 1, 1, '2026-03-15 18:29:16', '2026-03-15 18:29:16');
INSERT INTO forge_admin_new.sys_flow_form (id, form_key, form_name, form_type, form_schema, form_url, component_path,
                                           form_config, version, status, description, tenant_id, create_by, create_time,
                                           update_by, update_time, deleted)
VALUES (1, 'leave_form', '请假申请表单', 'dynamic',
        '[{"type":"input","field":"title","label":"申请标题","props":{"placeholder":"请输入标题"},"rules":[{"required":true,"message":"请输入标题"}]},{"type":"select","field":"leaveType","label":"请假类型","props":{"options":[{"label":"事假","value":"1"},{"label":"病假","value":"2"},{"label":"年假","value":"3"},{"label":"婚假","value":"4"},{"label":"产假","value":"5"}]},"rules":[{"required":true,"message":"请选择请假类型"}]},{"type":"datePicker","field":"startDate","label":"开始日期","rules":[{"required":true,"message":"请选择开始日期"}]},{"type":"datePicker","field":"endDate","label":"结束日期","rules":[{"required":true,"message":"请选择结束日期"}]},{"type":"inputNumber","field":"days","label":"请假天数","props":{"min":0.5,"precision":1},"rules":[{"required":true,"message":"请输入请假天数"}]},{"type":"textarea","field":"reason","label":"请假原因","props":{"placeholder":"请输入请假原因","rows":3},"rules":[{"required":true,"message":"请输入请假原因"}]}]',
        NULL, NULL, NULL, 1, 1, '员工请假申请表单', NULL, NULL, '2026-03-19 14:33:12', NULL, '2026-03-19 14:33:12', 0),
       (2, 'expense_form', '报销申请表单', 'dynamic',
        '[{"type":"input","field":"title","label":"报销标题","props":{"placeholder":"请输入报销标题"},"rules":[{"required":true,"message":"请输入报销标题"}]},{"type":"select","field":"expenseType","label":"报销类型","props":{"options":[{"label":"差旅费","value":"1"},{"label":"办公费","value":"2"},{"label":"招待费","value":"3"},{"label":"交通费","value":"4"},{"label":"其他","value":"5"}]},"rules":[{"required":true,"message":"请选择报销类型"}]},{"type":"inputNumber","field":"amount","label":"报销金额","props":{"min":0,"precision":2,"prefix":"¥"},"rules":[{"required":true,"message":"请输入报销金额"}]},{"type":"datePicker","field":"expenseDate","label":"报销日期","rules":[{"required":true,"message":"请选择报销日期"}]},{"type":"textarea","field":"description","label":"报销说明","props":{"placeholder":"请输入报销说明","rows":3}},{"type":"upload","field":"attachments","label":"附件","props":{"multiple":true,"accept":".jpg,.jpeg,.png,.pdf"}}]',
        NULL, NULL, NULL, 1, 1, '费用报销申请表单', NULL, NULL, '2026-03-19 14:33:12', NULL, '2026-03-19 14:33:12', 0),
       (3, 'approval_form', '通用审批表单', 'dynamic',
        '[{"type":"input","field":"title","label":"申请标题","props":{"placeholder":"请输入申请标题"},"rules":[{"required":true,"message":"请输入申请标题"}]},{"type":"select","field":"approvalType","label":"审批类型","props":{"options":[{"label":"通用审批","value":"1"},{"label":"合同审批","value":"2"},{"label":"项目审批","value":"3"},{"label":"其他","value":"4"}]},"rules":[{"required":true,"message":"请选择审批类型"}]},{"type":"textarea","field":"content","label":"申请内容","props":{"placeholder":"请输入申请内容","rows":4},"rules":[{"required":true,"message":"请输入申请内容"}]},{"type":"upload","field":"attachments","label":"附件","props":{"multiple":true}}]',
        NULL, NULL, NULL, 1, 1, '通用审批申请表单', NULL, NULL, '2026-03-19 14:33:12', NULL, '2026-03-19 14:33:12', 0);
INSERT INTO forge_admin_new.sys_flow_model (id, model_key, model_name, description, category, flow_type, form_type,
                                            form_id, form_json, version, process_definition_id, deployment_id,
                                            deployment_key, status, deploy_time, last_update_by, create_by, create_time,
                                            update_time, del_flag, bpmn_xml, notify_type, webhook_url)
VALUES ('2572074a70f3c1bac8d6d3bfe2484454', 'todo_flow', '请假流程', '', 'leave', '', 'none', NULL, '', 68,
        'todo_flow:34:42bb9900-252e-11f1-b539-c68306b388f0', '42b5f3ad-252e-11f1-b539-c68306b388f0', 'todo_flow_v67', 1,
        '2026-03-21 22:00:05', NULL, NULL, '2026-03-16 16:56:23', '2026-03-21 22:00:04', 0, '<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://flowable.org/processdef">
  <bpmn:process id="Process_1" name="新流程" isExecutable="true">
    <bpmn:startEvent id="StartEvent_1" name="开始">
      <bpmn:outgoing>Flow_0cw8tps</bpmn:outgoing>
    </bpmn:startEvent>
    <bpmn:sequenceFlow id="Flow_0cw8tps" sourceRef="StartEvent_1" targetRef="Activity_1lfbh0g" />
    <bpmn:userTask id="Activity_1lfbh0g" name="用户任务" flowable:assignee="${initiator}" flowable:candidateUsers="" flowable:formUrl="/test/approve12" flowable:formTarget="modal">
      <bpmn:extensionElements>
        <flowable:taskListener event="create" class="com.mdframe.forge.starter.flow.listener.TestListener" />
        <flowable:executionListener event="start" class="com.mdframe.forge.starter.flow.listener.TestListener" />
      </bpmn:extensionElements>
      <bpmn:incoming>Flow_0cw8tps</bpmn:incoming>
      <bpmn:outgoing>Flow_05q0uln</bpmn:outgoing>
    </bpmn:userTask>
    <bpmn:sequenceFlow id="Flow_05q0uln" sourceRef="Activity_1lfbh0g" targetRef="Event_0esht4l" />
    <bpmn:endEvent id="Event_0esht4l" name="结束">
      <bpmn:incoming>Flow_05q0uln</bpmn:incoming>
      <bpmn:terminateEventDefinition id="TerminateEventDefinition_0wpbthp" />
    </bpmn:endEvent>
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1">
      <bpmndi:BPMNShape id="StartEvent_1_di" bpmnElement="StartEvent_1">
        <dc:Bounds x="172" y="262" width="36" height="36" />
        <bpmndi:BPMNLabel>
          <dc:Bounds x="179" y="298" width="22" height="14" />
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Activity_1k77281_di" bpmnElement="Activity_1lfbh0g">
        <dc:Bounds x="260" y="240" width="100" height="80" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Event_1ceflgz_di" bpmnElement="Event_0esht4l">
        <dc:Bounds x="412" y="262" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="Flow_0cw8tps_di" bpmnElement="Flow_0cw8tps">
        <di:waypoint x="208" y="280" />
        <di:waypoint x="260" y="280" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_05q0uln_di" bpmnElement="Flow_05q0uln">
        <di:waypoint x="360" y="280" />
        <di:waypoint x="412" y="280" />
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>
', NULL, NULL),
       ('559ff4deba36ef466bffedd658c4e107', '22', '22', '2', 'leave', '', 'dynamic', NULL, NULL, 1, NULL, NULL, NULL, 0,
        NULL, NULL, NULL, '2026-03-25 17:07:52', '2026-03-25 17:16:45', 1, NULL, 'none', ''),
       ('f1c16641298465456fa120d9b59eb635', '11', '11', '', 'leave', '', 'none', NULL, NULL, 1, NULL, NULL, NULL, 0,
        NULL, NULL, NULL, '2026-03-25 17:07:44', '2026-03-25 17:16:42', 1, NULL, 'none', ''),
       ('ffd827049ebaee51c8564e12101eaadb', 'leave_process', '请假流程测试', '请假流程测试', 'leave', NULL, 'dynamic',
        NULL, '', 15, 'leave_process:10:08700850-28fe-11f1-9674-e6e756836190', '0869518d-28fe-11f1-9674-e6e756836190',
        'leave_process_v14', 1, '2026-03-26 18:24:56', NULL, NULL, '2026-03-22 20:16:51', '2026-03-26 19:20:19', 0, '<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xmlns:flowable="http://flowable.org/bpmn" xmlns:modeler="http://flowable.org/modeler" id="Definitions_1" targetNamespace="http://flowable.org/demo" exporter="Flowable Modeler" exporterVersion="1.0">
  <bpmn:process id="leave_process" name="请假流程" isExecutable="true">
    <bpmn:startEvent id="startEvent" name="开始">
      <bpmn:outgoing>flow_start_to_apply</bpmn:outgoing>
    </bpmn:startEvent>
    <bpmn:userTask id="approveTask" name="部门领导审批" flowable:assignee="${initiator}" flowable:formUrl="/leave/LeaveApproveForm">
      <bpmn:extensionElements />
      <bpmn:incoming>flow_start_to_apply</bpmn:incoming>
      <bpmn:outgoing>flow_approve_to_end</bpmn:outgoing>
    </bpmn:userTask>
    <bpmn:endEvent id="endEvent" name="结束">
      <bpmn:incoming>flow_approve_to_end</bpmn:incoming>
    </bpmn:endEvent>
    <bpmn:sequenceFlow id="flow_start_to_apply" name="提交申请" sourceRef="startEvent" targetRef="approveTask" />
    <bpmn:sequenceFlow id="flow_approve_to_end" name="完成" sourceRef="approveTask" targetRef="endEvent" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_leave_process">
    <bpmndi:BPMNPlane id="BPMNPlane_leave_process" bpmnElement="leave_process">
      <bpmndi:BPMNShape id="BPMNShape_startEvent" bpmnElement="startEvent">
        <dc:Bounds x="100" y="200" width="36" height="36" />
        <bpmndi:BPMNLabel>
          <dc:Bounds x="108" y="246" width="22" height="14" />
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="BPMNShape_approveTask" bpmnElement="approveTask">
        <dc:Bounds x="240" y="178" width="100" height="80" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="BPMNShape_endEvent" bpmnElement="endEvent">
        <dc:Bounds x="560" y="200" width="36" height="36" />
        <bpmndi:BPMNLabel>
          <dc:Bounds x="568" y="246" width="22" height="14" />
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="BPMNEdge_flow_start_to_apply" bpmnElement="flow_start_to_apply">
        <di:waypoint x="136" y="218" />
        <di:waypoint x="240" y="218" />
        <bpmndi:BPMNLabel>
          <dc:Bounds x="134" y="200" width="44" height="14" />
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="BPMNEdge_flow_approve_to_end" bpmnElement="flow_approve_to_end">
        <di:waypoint x="340" y="218" />
        <di:waypoint x="560" y="218" />
        <bpmndi:BPMNLabel>
          <dc:Bounds x="431" y="200" width="22" height="14" />
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>
', 'webhook', 'http://127.0.0.1:8080/leave/test');
INSERT INTO forge_admin_new.sys_flow_task (id, task_id, task_name, task_def_key, task_def_id, process_instance_id,
                                           process_def_id, process_def_key, business_key, business_type, title,
                                           assignee, assignee_name, candidate_users, candidate_groups, owner, due_date,
                                           priority, status, comment, attachment_urls, start_user_id, start_user_name,
                                           start_dept_id, start_dept_name, create_time, claim_time, complete_time)
VALUES ('0704f28c1cf702da3297459635b89564', '4500c3e9-2901-11f1-9444-16f80eb69e69', '部门领导审批', 'approveTask', NULL,
        '44fd4163-2901-11f1-9444-16f80eb69e69', 'leave_process:10:08700850-28fe-11f1-9674-e6e756836190',
        'leave_process', 'LEAVE_B6EC0266969B4DAA', 'leave', 'admin 的请假申请', '1', NULL, NULL, NULL, NULL, NULL, 50,
        2, '21313', NULL, '1', '超级管理员', '2', '研发部', '2026-03-26 18:48:06', NULL, '2026-03-26 18:49:08'),
       ('0764edb04a264f83326667cc308bec48', 'd0953b8a-24fe-11f1-9590-c68306b388f0', '用户任务', 'Activity_1lfbh0g',
        NULL, 'd092ca7b-24fe-11f1-9590-c68306b388f0', 'todo_flow:3:6dccf7a9-24f6-11f1-b21b-c68306b388f0', 'todo_flow',
        NULL, NULL, '用户任务 - todo_flow', '1', NULL, NULL, NULL, NULL, NULL, 50, 2, '121', NULL, NULL, NULL, NULL,
        NULL, '2026-03-21 16:20:27', NULL, '2026-03-21 16:43:25'),
       ('13b92f6d426ce892da288387709d6387', 'a24dc1a0-24f8-11f1-90cd-c68306b388f0', '用户任务', 'Activity_1lfbh0g',
        NULL, 'a249f101-24f8-11f1-90cd-c68306b388f0', 'todo_flow:3:6dccf7a9-24f6-11f1-b21b-c68306b388f0', 'todo_flow',
        NULL, NULL, '用户任务 - todo_flow', '1', NULL, NULL, NULL, NULL, NULL, 50, 2, '1231', NULL, NULL, NULL, NULL,
        NULL, '2026-03-21 15:36:12', NULL, '2026-03-21 17:41:32'),
       ('1fba444c987d7e0fae01c9200458ea36', '35cd8cd0-250a-11f1-b85c-c68306b388f0', '用户任务', 'Activity_1lfbh0g',
        NULL, '35cb69e1-250a-11f1-b85c-c68306b388f0', 'todo_flow:3:6dccf7a9-24f6-11f1-b21b-c68306b388f0', 'todo_flow',
        '1231312', 'todo_flow', 'todo_flow流程', '1', NULL, NULL, NULL, NULL, NULL, 50, 2, '额外企鹅群', NULL, '1',
        '超级管理员', '2', '研发部', '2026-03-21 17:42:01', NULL, '2026-03-21 17:54:08'),
       ('22c41eb2c68abe86a1314d8c6e49d11d', '3851a975-2516-11f1-b539-c68306b388f0', '用户任务', 'Activity_1lfbh0g',
        NULL, '384f1154-2516-11f1-b539-c68306b388f0', 'todo_flow:6:079e9e98-2516-11f1-a15c-c68306b388f0', 'todo_flow',
        '121212121', 'todo_flow', 'todo_flow流程', '1', NULL, NULL, NULL, NULL, NULL, 50, 0, NULL, NULL, '1',
        '超级管理员', '2', '研发部', '2026-03-21 19:07:59', NULL, NULL),
       ('369cbb00b3d260793a63f9d8c4f0f089', '55c0df8c-2903-11f1-a6c9-16f80eb69e69', '部门领导审批', 'approveTask', NULL,
        '55bfce06-2903-11f1-a6c9-16f80eb69e69', 'leave_process:10:08700850-28fe-11f1-9674-e6e756836190',
        'leave_process', 'LEAVE_9833FDF482904B01', 'leave', 'admin 的请假申请', '1', NULL, NULL, NULL, NULL, NULL, 50,
        2, '12313', NULL, '1', '超级管理员', '2', '研发部', '2026-03-26 19:02:53', NULL, '2026-03-26 19:03:05'),
       ('3be40254f9cd3016179fbd051cd3b423', '8621ea77-2909-11f1-9585-16f80eb69e69', '部门领导审批', 'approveTask', NULL,
        '861f7966-2909-11f1-9585-16f80eb69e69', 'leave_process:10:08700850-28fe-11f1-9674-e6e756836190',
        'leave_process', 'LEAVE_C055B79F68ED4079', 'leave', 'admin 的请假申请', '1', NULL, NULL, NULL, NULL, NULL, 50,
        0, NULL, NULL, '1', '超级管理员', '2', '研发部', '2026-03-26 19:47:11', NULL, NULL),
       ('4351f8a62f615cbf76b87e81f2c1e58f', 'c5d57148-2909-11f1-950e-16f80eb69e69', '部门领导审批', 'approveTask', NULL,
        'c5d263f7-2909-11f1-950e-16f80eb69e69', 'leave_process:10:08700850-28fe-11f1-9674-e6e756836190',
        'leave_process', 'LEAVE_71D5E9DF8ACD47CF', 'leave', 'admin 的请假申请', '1', NULL, NULL, NULL, NULL, NULL, 50,
        2, '12313', NULL, '1', '超级管理员', '2', '研发部', '2026-03-26 19:48:58', NULL, '2026-03-26 19:50:02'),
       ('46607e6e103a159c685c546a60300076', '58bf2060-252e-11f1-b539-c68306b388f0', '用户任务', 'Activity_1lfbh0g',
        NULL, '58b694d1-252e-11f1-b539-c68306b388f0', 'todo_flow:34:42bb9900-252e-11f1-b539-c68306b388f0', 'todo_flow',
        '1213123123123', 'todo_flow', 'todo_flow流程', '1', NULL, NULL, NULL, NULL, NULL, 50, 0, NULL, NULL, '1',
        '超级管理员', '2', '研发部', '2026-03-21 22:00:41', NULL, NULL),
       ('4f7b2b1308a02a2247f3e0c9ef4d89d1', 'dad23b0c-2902-11f1-a6c9-16f80eb69e69', '部门领导审批', 'approveTask', NULL,
        'dad177a6-2902-11f1-a6c9-16f80eb69e69', 'leave_process:10:08700850-28fe-11f1-9674-e6e756836190',
        'leave_process', 'LEAVE_AD395CD3AF304AD1', 'leave', 'admin 的请假申请', '1', NULL, NULL, NULL, NULL, NULL, 50,
        2, '12313', NULL, '1', '超级管理员', '2', '研发部', '2026-03-26 18:59:26', NULL, '2026-03-26 18:59:40');
INSERT INTO forge_admin_new.sys_flow_task (id, task_id, task_name, task_def_key, task_def_id, process_instance_id,
                                           process_def_id, process_def_key, business_key, business_type, title,
                                           assignee, assignee_name, candidate_users, candidate_groups, owner, due_date,
                                           priority, status, comment, attachment_urls, start_user_id, start_user_name,
                                           start_dept_id, start_dept_name, create_time, claim_time, complete_time)
VALUES ('551fc1528b2d9115f6e2782cc83f1686', 'cee7cfe8-2910-11f1-88d0-16f80eb69e69', '部门领导审批', 'approveTask', NULL,
        'cee537c7-2910-11f1-88d0-16f80eb69e69', 'leave_process:10:08700850-28fe-11f1-9674-e6e756836190',
        'leave_process', 'LEAVE_1CB98984B0B542AB', 'leave', 'admin 的请假申请', '1', NULL, NULL, NULL, NULL, NULL, 50,
        0, NULL, NULL, '1', '超级管理员', '2', '研发部', '2026-03-26 20:39:19', NULL, NULL),
       ('708398d6705e0039dfb07150ce3f5be5', 'cb35f6f1-2906-11f1-b933-16f80eb69e69', '部门领导审批', 'approveTask', NULL,
        'cb32264b-2906-11f1-b933-16f80eb69e69', 'leave_process:10:08700850-28fe-11f1-9674-e6e756836190',
        'leave_process', 'LEAVE_9BA9DECBFDB34DED', 'leave', 'admin 的请假申请', '1', NULL, NULL, NULL, NULL, NULL, 50,
        2, '111', NULL, '1', '超级管理员', '2', '研发部', '2026-03-26 19:27:38', NULL, '2026-03-26 19:29:21'),
       ('75d792db8d0d57183e0d1c828db743d5', 'fcd511df-24f7-11f1-828d-c68306b388f0', '用户任务', 'Activity_1lfbh0g',
        NULL, 'fcd0f320-24f7-11f1-828d-c68306b388f0', 'todo_flow:3:6dccf7a9-24f6-11f1-b21b-c68306b388f0', 'todo_flow',
        NULL, NULL, '用户任务 - todo_flow', '1', NULL, NULL, NULL, NULL, NULL, 50, 0, NULL, NULL, NULL, NULL, NULL,
        NULL, '2026-03-21 15:31:34', NULL, NULL),
       ('83b6abd0cb731f8d00054f06f6c08b1d', '21452647-28fe-11f1-9674-e6e756836190', '部门领导审批', 'approveTask', NULL,
        '2144b101-28fe-11f1-9674-e6e756836190', 'leave_process:10:08700850-28fe-11f1-9674-e6e756836190',
        'leave_process', 'LEAVE_84CB073379C3414D', 'leave', 'admin 的请假申请', '1', NULL, NULL, NULL, NULL, NULL, 50,
        3, '111', NULL, '1', '超级管理员', '2', '研发部', '2026-03-26 18:25:37', NULL, '2026-03-26 18:37:05'),
       ('966b04f8bebd3b9acfb7f7c4a5de67af', 'f9151b26-25ee-11f1-a191-c68306b388f0', '请假申请', 'applyTask', NULL,
        'f9112374-25ee-11f1-a191-c68306b388f0', 'leave_process:2:8cbcd859-25e9-11f1-90d0-c68306b388f0', 'leave_process',
        'LEAVE_C9B9D2736FE14D0C', 'leave', '年假申请 - admin', '1', NULL, NULL, NULL, NULL, NULL, 50, 0, NULL, NULL,
        '1', 'admin', NULL, NULL, '2026-03-22 20:59:34', NULL, NULL),
       ('a2cb8426f6e38b998d73b47f0cad2e70', '31bc321a-28fd-11f1-9674-e6e756836190', '部门领导审批', 'approveTask', NULL,
        '31b7ec44-28fd-11f1-9674-e6e756836190', 'leave_process:9:2a237673-28fd-11f1-9674-e6e756836190', 'leave_process',
        'LEAVE_5835B6CF47C547D7', 'leave', 'admin 的请假申请', '1', NULL, NULL, NULL, NULL, NULL, 50, 0, NULL, NULL,
        '1', '超级管理员', '2', '研发部', '2026-03-26 18:18:55', NULL, NULL),
       ('aef2a367039cb78b1fc8a03e437a516a', '5cd03c0c-2902-11f1-a6c9-16f80eb69e69', '部门领导审批', 'approveTask', NULL,
        '5ccfc6c6-2902-11f1-a6c9-16f80eb69e69', 'leave_process:10:08700850-28fe-11f1-9674-e6e756836190',
        'leave_process', 'LEAVE_7EC50E86F49B4A09', 'leave', 'admin 的请假申请', '1', NULL, NULL, NULL, NULL, NULL, 50,
        2, '12313', NULL, '1', '超级管理员', '2', '研发部', '2026-03-26 18:55:55', NULL, '2026-03-26 18:56:13'),
       ('afd6728312c6879f2b5ee38c9d1938b9', '078158c9-28f2-11f1-8984-e6e756836190', '部门领导审批', 'approveTask', NULL,
        '077c0183-28f2-11f1-8984-e6e756836190', 'leave_process:7:edaf9ac8-28eb-11f1-810f-e6e756836190', 'leave_process',
        'LEAVE_83FB20E0F70E4ACD', 'leave', 'admin 的请假申请', '2', NULL, NULL, NULL, NULL, NULL, 50, 2, '1231', NULL,
        '1', '超级管理员', '2', '研发部', '2026-03-26 16:59:00', NULL, '2026-03-26 17:16:31'),
       ('ba542fa02ac5cc7c7eee5c4d575185a2', 'cd7d6b84-2905-11f1-a323-16f80eb69e69', '部门领导审批', 'approveTask', NULL,
        'cd7973ce-2905-11f1-a323-16f80eb69e69', 'leave_process:10:08700850-28fe-11f1-9674-e6e756836190',
        'leave_process', 'LEAVE_F752BF1F5D234E34', 'leave', 'admin 的请假申请', '1', NULL, NULL, NULL, NULL, NULL, 50,
        2, '1231', NULL, '1', '超级管理员', '2', '研发部', '2026-03-26 19:20:33', NULL, '2026-03-26 19:24:30'),
       ('e7ac7185200a8f4d765cfc97721e0e88', 'a3638419-2901-11f1-9444-16f80eb69e69', '部门领导审批', 'approveTask', NULL,
        'a362c0b3-2901-11f1-9444-16f80eb69e69', 'leave_process:10:08700850-28fe-11f1-9674-e6e756836190',
        'leave_process', 'LEAVE_3804972B33BE410F', 'leave', 'admin 的请假申请', '1', NULL, NULL, NULL, NULL, NULL, 50,
        2, '12313', NULL, '1', '超级管理员', '2', '研发部', '2026-03-26 18:50:44', NULL, '2026-03-26 18:50:56');
INSERT INTO forge_admin_new.sys_flow_task (id, task_id, task_name, task_def_key, task_def_id, process_instance_id,
                                           process_def_id, process_def_key, business_key, business_type, title,
                                           assignee, assignee_name, candidate_users, candidate_groups, owner, due_date,
                                           priority, status, comment, attachment_urls, start_user_id, start_user_name,
                                           start_dept_id, start_dept_name, create_time, claim_time, complete_time)
VALUES ('ef526e05eafe14e084847816b88fe23e', '1898179c-2902-11f1-a6c9-16f80eb69e69', '部门领导审批', 'approveTask', NULL,
        '18949516-2902-11f1-a6c9-16f80eb69e69', 'leave_process:10:08700850-28fe-11f1-9674-e6e756836190',
        'leave_process', 'LEAVE_76B0FB0F24504EDD', 'leave', 'admin 的请假申请', '1', NULL, NULL, NULL, NULL, NULL, 50,
        2, '12313', NULL, '1', '超级管理员', '2', '研发部', '2026-03-26 18:54:01', NULL, '2026-03-26 18:54:48'),
       ('fb982b1257c7e8ed793e1e8374c591d2', 'c2e6a7c3-2904-11f1-b7a8-16f80eb69e69', '部门领导审批', 'approveTask', NULL,
        'c2e261ed-2904-11f1-b7a8-16f80eb69e69', 'leave_process:10:08700850-28fe-11f1-9674-e6e756836190',
        'leave_process', 'LEAVE_8E319D3F139243D6', 'leave', 'admin 的请假申请', '1', NULL, NULL, NULL, NULL, NULL, 50,
        2, '1231', NULL, '1', '超级管理员', '2', '研发部', '2026-03-26 19:13:05', NULL, '2026-03-26 19:13:18');
INSERT INTO forge_admin_new.sys_flow_template (id, template_key, template_name, category, description, icon, form_type,
                                               form_json, bpmn_xml, thumbnail, variables, version, status, usage_count,
                                               create_by, create_time, update_time, is_system, sort_order)
VALUES ('1', 'leave_simple', '简单请假流程', 'leave', '适用于短期请假（3天以内），只需直属领导审批',
        'i-material-symbols:event', 'dynamic',
        '[{"type":"input","field":"title","label":"申请标题","props":{"placeholder":"请输入标题"},"rules":[{"required":true,"message":"请输入标题"}]},{"type":"select","field":"leaveType","label":"请假类型","props":{"options":[{"label":"事假","value":"1"},{"label":"病假","value":"2"},{"label":"年假","value":"3"}]}},{"type":"datePicker","field":"startDate","label":"开始日期"},{"type":"datePicker","field":"endDate","label":"结束日期"},{"type":"inputNumber","field":"days","label":"请假天数"},{"type":"textarea","field":"reason","label":"请假原因"}]',
        '<?xml version="1.0" encoding="UTF-8"?><bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://flowable.org/processdef"><bpmn:process id="leave_simple" name="简单请假流程" isExecutable="true"><bpmn:startEvent id="startEvent" name="开始" flowable:initiator="initiator"/><bpmn:userTask id="approveTask" name="直属领导审批" flowable:assignee="${initiatorLeader}"/><bpmn:endEvent id="endEvent" name="结束"/><bpmn:sequenceFlow id="flow1" sourceRef="startEvent" targetRef="approveTask"/><bpmn:sequenceFlow id="flow2" sourceRef="approveTask" targetRef="endEvent"/></bpmn:process><bpmndi:BPMNDiagram id="BPMNDiagram_1"><bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="leave_simple"><bpmndi:BPMNShape id="startEvent_di" bpmnElement="startEvent"><dc:Bounds x="180" y="160" width="36" height="36"/></bpmndi:BPMNShape><bpmndi:BPMNShape id="approveTask_di" bpmnElement="approveTask"><dc:Bounds x="280" y="138" width="100" height="80"/></bpmndi:BPMNShape><bpmndi:BPMNShape id="endEvent_di" bpmnElement="endEvent"><dc:Bounds x="460" y="160" width="36" height="36"/></bpmndi:BPMNShape></bpmndi:BPMNPlane></bpmndi:BPMNDiagram></bpmn:definitions>',
        NULL, NULL, 1, 1, 0, NULL, '2026-03-19 14:33:12', '2026-03-19 14:33:12', '1', 1),
       ('2', 'leave_multi', '多级请假流程', 'leave', '适用于长期请假（3天以上），需要多级领导审批',
        'i-material-symbols:event-note', 'dynamic',
        '[{"type":"input","field":"title","label":"申请标题","props":{"placeholder":"请输入标题"},"rules":[{"required":true,"message":"请输入标题"}]},{"type":"select","field":"leaveType","label":"请假类型","props":{"options":[{"label":"事假","value":"1"},{"label":"病假","value":"2"},{"label":"年假","value":"3"}]}},{"type":"datePicker","field":"startDate","label":"开始日期"},{"type":"datePicker","field":"endDate","label":"结束日期"},{"type":"inputNumber","field":"days","label":"请假天数"},{"type":"textarea","field":"reason","label":"请假原因"}]',
        '<?xml version="1.0" encoding="UTF-8"?><bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://flowable.org/processdef"><bpmn:process id="leave_multi" name="多级请假流程" isExecutable="true"><bpmn:startEvent id="startEvent" name="开始" flowable:initiator="initiator"/><bpmn:userTask id="deptApprove" name="部门经理审批" flowable:assignee="${deptManager}"/><bpmn:userTask id="hrApprove" name="HR审批" flowable:candidateGroups="${hr}"/><bpmn:endEvent id="endEvent" name="结束"/><bpmn:sequenceFlow id="flow1" sourceRef="startEvent" targetRef="deptApprove"/><bpmn:sequenceFlow id="flow2" sourceRef="deptApprove" targetRef="hrApprove"/><bpmn:sequenceFlow id="flow3" sourceRef="hrApprove" targetRef="endEvent"/></bpmn:process><bpmndi:BPMNDiagram id="BPMNDiagram_1"><bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="leave_multi"><bpmndi:BPMNShape id="startEvent_di" bpmnElement="startEvent"><dc:Bounds x="180" y="160" width="36" height="36"/></bpmndi:BPMNShape><bpmndi:BPMNShape id="deptApprove_di" bpmnElement="deptApprove"><dc:Bounds x="280" y="138" width="100" height="80"/></bpmndi:BPMNShape><bpmndi:BPMNShape id="hrApprove_di" bpmnElement="hrApprove"><dc:Bounds x="440" y="138" width="100" height="80"/></bpmndi:BPMNShape><bpmndi:BPMNShape id="endEvent_di" bpmnElement="endEvent"><dc:Bounds x="620" y="160" width="36" height="36"/></bpmndi:BPMNShape></bpmndi:BPMNPlane></bpmndi:BPMNDiagram></bpmn:definitions>',
        NULL, NULL, 1, 1, 0, NULL, '2026-03-19 14:33:12', '2026-03-19 14:33:12', '1', 2),
       ('3', 'expense_common', '通用报销流程', 'expense', '适用于日常费用报销，如差旅费、办公费等',
        'i-material-symbols:payments', 'dynamic',
        '[{"type":"input","field":"title","label":"报销标题","rules":[{"required":true,"message":"请输入标题"}]},{"type":"select","field":"expenseType","label":"报销类型","props":{"options":[{"label":"差旅费","value":"1"},{"label":"办公费","value":"2"},{"label":"招待费","value":"3"},{"label":"其他","value":"4"}]}},{"type":"inputNumber","field":"amount","label":"报销金额","props":{"precision":2}},{"type":"datePicker","field":"expenseDate","label":"报销日期"},{"type":"textarea","field":"description","label":"报销说明"},{"type":"upload","field":"attachments","label":"附件","props":{"multiple":true}}]',
        '<?xml version="1.0" encoding="UTF-8"?><bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://flowable.org/processdef"><bpmn:process id="expense_common" name="通用报销流程" isExecutable="true"><bpmn:startEvent id="startEvent" name="开始" flowable:initiator="initiator"/><bpmn:userTask id="approveTask" name="审批" flowable:assignee="${initiatorLeader}"/><bpmn:userTask id="financeTask" name="财务审核" flowable:candidateGroups="finance"/><bpmn:endEvent id="endEvent" name="结束"/><bpmn:sequenceFlow id="flow1" sourceRef="startEvent" targetRef="approveTask"/><bpmn:sequenceFlow id="flow2" sourceRef="approveTask" targetRef="financeTask"/><bpmn:sequenceFlow id="flow3" sourceRef="financeTask" targetRef="endEvent"/></bpmn:process><bpmndi:BPMNDiagram id="BPMNDiagram_1"><bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="expense_common"><bpmndi:BPMNShape id="startEvent_di" bpmnElement="startEvent"><dc:Bounds x="180" y="160" width="36" height="36"/></bpmndi:BPMNShape><bpmndi:BPMNShape id="approveTask_di" bpmnElement="approveTask"><dc:Bounds x="280" y="138" width="100" height="80"/></bpmndi:BPMNShape><bpmndi:BPMNShape id="financeTask_di" bpmnElement="financeTask"><dc:Bounds x="440" y="138" width="100" height="80"/></bpmndi:BPMNShape><bpmndi:BPMNShape id="endEvent_di" bpmnElement="endEvent"><dc:Bounds x="620" y="160" width="36" height="36"/></bpmndi:BPMNShape></bpmndi:BPMNPlane></bpmndi:BPMNDiagram></bpmn:definitions>',
        NULL, NULL, 1, 1, 0, NULL, '2026-03-19 14:33:12', '2026-03-19 14:33:12', '1', 3),
       ('4', 'approval_simple', '通用审批流程', 'approval', '适用于简单的审批场景，单级审批',
        'i-material-symbols:approval', 'dynamic',
        '[{"type":"input","field":"title","label":"申请标题","rules":[{"required":true,"message":"请输入标题"}]},{"type":"textarea","field":"content","label":"申请内容"},{"type":"upload","field":"attachments","label":"附件"}]',
        '<?xml version="1.0" encoding="UTF-8"?><bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://flowable.org/processdef"><bpmn:process id="approval_simple" name="通用审批流程" isExecutable="true"><bpmn:startEvent id="startEvent" name="开始" flowable:initiator="initiator"/><bpmn:userTask id="approveTask" name="审批" flowable:assignee="${approver}"/><bpmn:endEvent id="endEvent" name="结束"/><bpmn:sequenceFlow id="flow1" sourceRef="startEvent" targetRef="approveTask"/><bpmn:sequenceFlow id="flow2" sourceRef="approveTask" targetRef="endEvent"/></bpmn:process><bpmndi:BPMNDiagram id="BPMNDiagram_1"><bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="approval_simple"><bpmndi:BPMNShape id="startEvent_di" bpmnElement="startEvent"><dc:Bounds x="180" y="160" width="36" height="36"/></bpmndi:BPMNShape><bpmndi:BPMNShape id="approveTask_di" bpmnElement="approveTask"><dc:Bounds x="280" y="138" width="100" height="80"/></bpmndi:BPMNShape><bpmndi:BPMNShape id="endEvent_di" bpmnElement="endEvent"><dc:Bounds x="460" y="160" width="36" height="36"/></bpmndi:BPMNShape></bpmndi:BPMNPlane></bpmndi:BPMNDiagram></bpmn:definitions>',
        NULL, NULL, 1, 1, 0, NULL, '2026-03-19 14:33:12', '2026-03-19 14:33:12', '1', 4);
INSERT INTO forge_admin_new.sys_job_config (id, job_name, job_group, description, executor_bean, executor_method,
                                            executor_handler, executor_service, cron_expression, job_param, status,
                                            execute_mode, retry_count, alarm_email, webhook_url, create_time,
                                            update_time)
VALUES (3, 'paramTask', 'DEFAULT', '每10分钟执行', 'jobExamples', 'taskWithParam', NULL, NULL, '0 0/10 * * * ?', NULL,
        0, 'BEAN', 0, NULL, NULL, '2025-12-02 15:44:08', '2025-12-02 15:44:08'),
       (4, 'simpleTask', 'DEFAULT', '每5秒执行一次', 'jobExamples', 'simpleTask', NULL, NULL, '0/5 * * * * ?', NULL, 0,
        'BEAN', 0, NULL, NULL, '2025-12-02 15:44:09', '2025-12-02 15:44:09');
INSERT INTO forge_admin_new.sys_org (id, tenant_id, org_name, parent_id, ancestors, sort, org_type, org_status,
                                     leader_id, leader_name, phone, address, region_code, remark, create_by,
                                     create_time, update_by, update_time, create_dept)
VALUES (1, 1, '默认集团', 0, '0', 1, 1, 1, 1, '超级管理员', NULL, NULL, NULL, NULL, NULL, '2025-11-12 17:41:18', NULL,
        '2025-11-12 17:41:18', NULL),
       (2, 1, '研发部', 1, '0,1', 1, 2, 1, 2, '测试用户', NULL, NULL, NULL, NULL, NULL, '2025-11-12 17:41:18', NULL,
        '2025-11-12 17:41:18', NULL),
       (3, 1, '测试组', 2, '0,1,2', 1, 3, 1, 2, '测试用户', NULL, NULL, NULL, NULL, NULL, '2025-11-12 17:41:18', NULL,
        '2025-12-09 17:19:53', NULL);
INSERT INTO forge_admin_new.sys_post (id, tenant_id, post_code, org_id, post_name, post_status, post_type, sort, remark,
                                      create_by, create_time, update_by, update_time, create_dept)
VALUES (1, 1, 'CEO', 1, '首席执行官11', 1, 1, 1, '公司最高管理者', NULL, '2025-11-12 17:41:18', NULL,
        '2025-12-09 17:27:13', NULL),
       (2, 1, 'CTO', 1, '技术总监', 1, 1, 2, '技术部门负责人', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18',
        NULL),
       (3, 1, 'DEV', 2, '开发工程师', 1, 2, 1, '软件开发工程师', NULL, '2025-11-12 17:41:18', NULL,
        '2025-11-12 17:41:18', NULL),
       (4, 1, 'QA', 2, '测试工程师', 1, 2, 2, '软件测试工程师', NULL, '2025-11-12 17:41:18', NULL,
        '2025-11-12 17:41:18', NULL);
INSERT INTO forge_admin_new.sys_resource (id, tenant_id, resource_name, parent_id, resource_type, sort, `path`,
                                          component, is_external, is_public, menu_status, visible, perms, icon,
                                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by,
                                          create_time, update_by, update_time, create_dept)
VALUES (1, 1, '系统管理', 0, 1, 1, '/system', NULL, 0, 0, 1, 1, NULL, 'ionicons5:Albums', NULL, NULL, 0, 1, NULL,
        '系统管理目录', NULL, '2025-11-12 17:41:18', 1, '2026-01-20 16:50:28', NULL),
       (2, 1, '用户管理', 1, 2, 1, '/system/user', 'system/user', 0, 0, 1, 1, 'system:user:list',
        'ionicons5:AccessibilityOutline', NULL, NULL, 1, 0, NULL, '用户管理菜单', NULL, '2025-11-12 17:41:18', NULL,
        '2026-03-22 17:57:19', NULL),
       (3, 1, '用户查询', 2, 3, 1, NULL, NULL, 0, 0, 1, 1, 'system:user:query', NULL, NULL, NULL, 0, 0, NULL,
        '用户查询按钮', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL),
       (4, 1, '用户新增', 2, 3, 2, NULL, NULL, 0, 0, 1, 1, 'system:user:add', NULL, NULL, NULL, 0, 0, NULL,
        '用户新增按钮', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL),
       (5, 1, '用户修改', 2, 3, 3, NULL, NULL, 0, 0, 1, 1, 'system:user:edit', NULL, NULL, NULL, 0, 0, NULL,
        '用户修改按钮', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL),
       (6, 1, '用户删除', 2, 3, 4, NULL, NULL, 0, 0, 1, 1, 'system:user:remove', NULL, NULL, NULL, 0, 0, NULL,
        '用户删除按钮', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL),
       (7, 1, '用户分页查询API', 2, 4, 1, NULL, NULL, 0, 0, 1, 1, 'system:user:page', NULL, 'GET', '/system/user/page',
        0, 0, NULL, '用户分页查询接口', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL),
       (8, 1, '用户新增API', 2, 4, 2, NULL, NULL, 0, 0, 1, 1, 'system:user:add', NULL, 'POST', '/system/user/add', 0, 0,
        NULL, '用户新增接口', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL),
       (9, 1, '角色管理', 1, 2, 2, '/system/role', 'system/role', 0, 0, 1, 1, 'system:role:list',
        'local:ai-icon:layers', NULL, NULL, 1, 0, NULL, '角色管理菜单', NULL, '2025-11-12 17:41:18', NULL,
        '2025-12-09 17:18:50', NULL),
       (10, 1, '角色查询', 9, 3, 1, NULL, NULL, 0, 0, 1, 1, 'system:role:query', NULL, NULL, NULL, 0, 0, NULL,
        '角色查询按钮', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL);
INSERT INTO forge_admin_new.sys_resource (id, tenant_id, resource_name, parent_id, resource_type, sort, `path`,
                                          component, is_external, is_public, menu_status, visible, perms, icon,
                                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by,
                                          create_time, update_by, update_time, create_dept)
VALUES (11, 1, '角色新增', 9, 3, 2, NULL, NULL, 0, 0, 1, 1, 'system:role:add', NULL, NULL, NULL, 0, 0, NULL,
        '角色新增按钮', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL),
       (12, 1, '角色修改', 9, 3, 3, NULL, NULL, 0, 0, 1, 1, 'system:role:edit', NULL, NULL, NULL, 0, 0, NULL,
        '角色修改按钮', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL),
       (13, 1, '角色删除', 9, 3, 4, NULL, NULL, 0, 0, 1, 1, 'system:role:remove', NULL, NULL, NULL, 0, 0, NULL,
        '角色删除按钮', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL),
       (14, 1, '组织管理', 1, 2, 3, '/system/org', 'system/org', 0, 0, 1, 1, 'system:org:list',
        'local:ai-icon:briefcase', NULL, NULL, 1, 0, NULL, '组织管理菜单', NULL, '2025-11-12 17:41:18', NULL,
        '2025-12-09 17:17:10', NULL),
       (15, 1, '组织查询', 14, 3, 1, NULL, NULL, 0, 0, 1, 1, 'system:org:query', NULL, NULL, NULL, 0, 0, NULL,
        '组织查询按钮', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL),
       (16, 1, '组织新增', 14, 3, 2, NULL, NULL, 0, 0, 1, 1, 'system:org:add', NULL, NULL, NULL, 0, 0, NULL,
        '组织新增按钮', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL),
       (17, 1, '岗位管理', 1, 2, 4, '/system/post', 'system/post', 0, 0, 1, 1, 'system:post:list', 'ionicons5:Albums',
        NULL, NULL, 1, 0, NULL, '岗位管理菜单', NULL, '2025-11-12 17:41:18', NULL, '2025-12-09 17:23:11', NULL),
       (18, 1, '岗位查询', 17, 3, 1, NULL, NULL, 0, 0, 1, 1, 'system:post:query', NULL, NULL, NULL, 0, 0, NULL,
        '岗位查询按钮', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL),
       (20, 1, '查询案件信息', 19, 3, 1, NULL, NULL, 0, 0, 1, 1, 'system:legalCase:query', NULL, NULL, NULL, 0, 0, NULL,
        '查询案件信息按钮', NULL, '2025-12-05 14:37:17', NULL, '2025-12-09 10:28:31', NULL),
       (21, 1, '新增案件信息', 19, 3, 2, NULL, NULL, 0, 0, 1, 1, 'system:legalCase:add', NULL, NULL, NULL, 0, 0, NULL,
        '新增案件信息按钮', NULL, '2025-12-05 14:37:17', NULL, '2025-12-08 16:04:23', NULL);
INSERT INTO forge_admin_new.sys_resource (id, tenant_id, resource_name, parent_id, resource_type, sort, `path`,
                                          component, is_external, is_public, menu_status, visible, perms, icon,
                                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by,
                                          create_time, update_by, update_time, create_dept)
VALUES (22, 1, '修改案件信息', 19, 3, 3, NULL, NULL, 0, 0, 1, 1, 'system:legalCase:edit', NULL, NULL, NULL, 0, 0, NULL,
        '修改案件信息按钮', NULL, '2025-12-05 14:37:17', NULL, '2025-12-08 16:04:24', NULL),
       (23, 1, '删除案件信息', 19, 3, 4, NULL, NULL, 0, 0, 1, 1, 'system:legalCase:remove', NULL, NULL, NULL, 0, 0,
        NULL, '删除案件信息按钮', NULL, '2025-12-05 14:37:17', NULL, '2025-12-08 16:04:24', NULL),
       (24, 1, '菜单管理', 1, 2, 4, '/system/menu', 'system/menu', 0, 0, 1, 1, 'system:menu:list', 'ionicons5:Menu',
        NULL, NULL, 1, 0, NULL, '菜单管理', NULL, '2025-11-12 17:41:18', NULL, '2025-12-09 17:17:48', NULL),
       (26, 1, '租户管理', 1, 2, 6, '/system/tenant', 'system/tenant', 0, 0, 1, 1, NULL, 'ionicons5:MapOutline', 'GET',
        NULL, 0, 0, NULL, NULL, NULL, '2025-12-09 17:32:06', NULL, '2025-12-09 17:37:07', NULL),
       (27, 1, '字典管理', 44, 2, 7, '/system/dictType', 'system/dictType', 0, 0, 1, 1, NULL,
        'ionicons5:MedicalOutline', 'GET', NULL, 0, 0, NULL, NULL, NULL, '2025-12-11 16:01:06', 1,
        '2026-01-21 17:37:37', NULL),
       (28, 1, '字典数据', 44, 2, 8, '/system/dictData', 'system/dictData', 0, 0, 0, 0, NULL, 'ionicons5:Dice', 'GET',
        NULL, 0, 0, NULL, NULL, NULL, '2025-12-11 16:06:09', 1, '2026-03-02 16:15:24', NULL),
       (29, 1, '参数设置', 44, 2, 8, '/system/config', 'system/config', 0, 0, 1, 1, NULL, 'ionicons5:Apps', 'GET', NULL,
        0, 0, NULL, NULL, NULL, '2025-12-11 17:21:09', 1, '2026-01-21 17:37:43', NULL),
       (30, 1, '通知公告', 43, 2, 9, '/system/notice', 'system/notice', 0, 0, 1, 1, NULL,
        'ionicons5:NotificationsOutline', 'GET', NULL, 0, 0, NULL, NULL, NULL, '2025-12-12 09:34:50', 1,
        '2026-01-21 15:55:48', NULL),
       (31, 1, '数据权限配置', 44, 2, 5, '/system/dataScopeConfig', 'system/dataScopeConfig', 0, 0, 1, 1,
        'system:dataScope:list', 'ionicons5:HandRight', NULL, NULL, 0, 0, NULL, '数据权限配置管理', 1,
        '2026-01-20 15:06:29', 1, '2026-03-16 17:28:08', NULL),
       (39, 1, '代码生成', 0, 1, 2, '/generator', NULL, 0, 0, 1, 1, NULL, 'ionicons5:LogoCodepen', 'GET', NULL, 0, 0,
        NULL, NULL, 1, '2026-01-20 16:50:19', 1, '2026-01-20 16:50:27', 1);
INSERT INTO forge_admin_new.sys_resource (id, tenant_id, resource_name, parent_id, resource_type, sort, `path`,
                                          component, is_external, is_public, menu_status, visible, perms, icon,
                                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by,
                                          create_time, update_by, update_time, create_dept)
VALUES (40, 1, '数据源配置', 39, 2, 0, '/generator/datasource', 'generator/datasource', 0, 0, 1, 1, NULL,
        'ionicons5:GridSharp', 'GET', NULL, 0, 0, NULL, NULL, 1, '2026-01-20 16:51:58', 1, '2026-01-20 16:51:58', 1),
       (41, 1, '表模型管理', 39, 2, 0, '/generator/table', 'generator/table', 0, 0, 1, 1, NULL,
        'ionicons5:TabletLandscape', 'GET', NULL, 0, 0, NULL, NULL, 1, '2026-01-20 16:53:04', 1, '2026-01-20 16:53:04',
        1),
       (43, 1, '日常办公', 0, 1, 3, '/work', NULL, 0, 0, 1, 1, NULL, 'ionicons5:BagCheck', 'GET', NULL, 0, 0, NULL,
        NULL, 1, '2026-01-21 15:55:31', 1, '2026-01-21 15:55:31', 1),
       (44, 1, '配置管理', 0, 1, 4, '/config', NULL, 0, 0, 1, 1, NULL, 'ionicons5:ConstructSharp', 'GET', NULL, 0, 0,
        NULL, NULL, 1, '2026-01-21 16:24:25', 1, '2026-01-21 16:24:25', 1),
       (45, 1, '存储配置', 44, 2, 1, '/system/storage-config', 'system/storage-config', 0, 0, 1, 1, NULL,
        'ionicons5:FileTray', 'GET', NULL, 0, 0, NULL, NULL, 1, '2026-01-21 16:24:55', 1, '2026-01-21 16:24:55', 1),
       (46, 1, '文件管理', 44, 2, 2, '/system/file-list', 'system/file-list', 0, 0, 1, 1, NULL,
        'ionicons5:FileTrayStackedSharp', 'GET', NULL, 0, 0, NULL, NULL, 1, '2026-01-21 16:25:41', 1,
        '2026-01-21 16:26:24', 1),
       (47, 1, 'Excel导出配置', 44, 2, 6, '/system/excel-export-config', 'system/excel-export-config', 0, 0, 1, 1, NULL,
        'ionicons5:ExitSharp', 'GET', NULL, 0, 0, NULL, NULL, 1, '2026-01-21 22:34:08', 1, '2026-01-21 22:34:08', 2),
       (48, 1, '消息管理', 0, 1, 5, '/message', NULL, 0, 0, 1, 1, NULL, 'ionicons5:NotificationsCircleSharp', 'GET',
        NULL, 0, 0, NULL, NULL, 1, '2026-01-31 20:19:24', 1, '2026-02-03 17:24:35', 2),
       (49, 1, '模版配置', 48, 2, 0, '/message/template-list', 'message/template-list', 0, 0, 1, 1, NULL,
        'ionicons5:AddCircleSharp', 'GET', NULL, 0, 0, NULL, NULL, 1, '2026-01-31 20:19:59', 1, '2026-02-03 17:24:54',
        2),
       (50, 1, '消息列表', 48, 2, 0, '/message/message-list', 'message/message-list', 0, 0, 1, 1, NULL,
        'ionicons5:TabletLandscape', 'GET', NULL, 0, 0, NULL, NULL, 1, '2026-01-31 20:20:39', 1, '2026-02-03 17:26:33',
        2);
INSERT INTO forge_admin_new.sys_resource (id, tenant_id, resource_name, parent_id, resource_type, sort, `path`,
                                          component, is_external, is_public, menu_status, visible, perms, icon,
                                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by,
                                          create_time, update_by, update_time, create_dept)
VALUES (53, 1, '测试222', 52, 2, 0, '/test/template', 'test/template', 0, 0, 1, 1, NULL, NULL, 'GET', NULL, 0, 0, NULL,
        NULL, 1, '2026-02-03 17:25:57', 1, '2026-02-03 17:37:12', 2),
       (54, 1, '系统监控', 0, 1, 7, '/system/monitor', NULL, 0, 0, 1, 1, NULL, 'ionicons5:InvertModeSharp', 'GET', NULL,
        0, 0, NULL, NULL, 1, '2026-02-04 11:52:09', 1, '2026-02-04 11:52:17', 2),
       (55, 1, '定时任务配置', 44, 2, 0, '/system/job-config', 'system/job-config', 0, 0, 1, 1, NULL,
        'ionicons5:TimeSharp', 'GET', NULL, 0, 0, NULL, NULL, 1, '2026-02-04 11:52:50', 1, '2026-03-02 14:27:13', 2),
       (56, 1, '登录日志', 54, 2, 3, '/system/login-log', 'system/login-log', 0, 0, 1, 1, NULL,
        'ionicons5:LogoFirebase', 'GET', NULL, 0, 0, NULL, NULL, 1, '2026-02-04 14:40:48', 1, '2026-03-02 16:13:25', 2),
       (57, 1, '操作日志', 54, 2, 2, '/system/operation-log', 'system/operation-log', 0, 0, 1, 1, NULL,
        'ionicons5:LogoInstagram', 'GET', NULL, 0, 0, NULL, NULL, 1, '2026-02-04 14:41:26', 1, '2026-02-04 14:41:26',
        2),
       (58, 1, '缓存监控', 54, 2, 1, '/system/cache', 'system/cache', 0, 0, 1, 1, 'system:cache:list',
        'ionicons5:LogoGooglePlaystore', NULL, NULL, 1, 0, NULL, '缓存管理菜单', 1, '2026-02-04 15:25:05', 1,
        '2026-03-02 16:13:22', NULL),
       (59, 1, '缓存查询', 58, 3, 1, NULL, NULL, 0, 0, 1, 1, 'system:cache:query', NULL, NULL, NULL, 0, 0, NULL,
        '查询缓存按钮', 1, '2026-02-04 15:25:05', 1, '2026-02-04 15:25:27', NULL),
       (60, 1, '缓存删除', 58, 3, 2, NULL, NULL, 0, 0, 1, 1, 'system:cache:remove', NULL, NULL, NULL, 0, 0, NULL,
        '删除缓存按钮', 1, '2026-02-04 15:25:06', 1, '2026-02-04 15:25:27', NULL),
       (61, 1, '清空缓存', 58, 3, 3, NULL, NULL, 0, 0, 1, 1, 'system:cache:clear', NULL, NULL, NULL, 0, 0, NULL,
        '清空缓存按钮', 1, '2026-02-04 15:25:06', 1, '2026-02-04 15:25:27', NULL),
       (62, 1, '缓存-分页查询', 58, 4, 1, NULL, NULL, 0, 0, 1, 1, 'system:cache:page', NULL, 'GET',
        '/system/cache/page', 0, 0, NULL, '分页查询缓存列表', 1, '2026-02-04 15:25:06', 1, '2026-02-04 15:25:27', NULL);
INSERT INTO forge_admin_new.sys_resource (id, tenant_id, resource_name, parent_id, resource_type, sort, `path`,
                                          component, is_external, is_public, menu_status, visible, perms, icon,
                                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by,
                                          create_time, update_by, update_time, create_dept)
VALUES (63, 1, '缓存-详情查询', 58, 4, 2, NULL, NULL, 0, 0, 1, 1, 'system:cache:getInfo', NULL, 'POST',
        '/system/cache/getInfo', 0, 0, NULL, '查询缓存详细信息', 1, '2026-02-04 15:25:06', 1, '2026-02-04 15:25:27',
        NULL),
       (64, 1, '缓存-删除', 58, 4, 3, NULL, NULL, 0, 0, 1, 1, 'system:cache:remove', NULL, 'POST',
        '/system/cache/remove', 0, 0, NULL, '删除单个缓存', 1, '2026-02-04 15:25:06', 1, '2026-02-04 15:25:27', NULL),
       (65, 1, '缓存-批量删除', 58, 4, 4, NULL, NULL, 0, 0, 1, 1, 'system:cache:removeBatch', NULL, 'POST',
        '/system/cache/removeBatch', 0, 0, NULL, '批量删除缓存', 1, '2026-02-04 15:25:06', 1, '2026-02-04 15:25:27',
        NULL),
       (66, 1, '缓存-清空', 58, 4, 5, NULL, NULL, 0, 0, 1, 1, 'system:cache:clear', NULL, 'POST', '/system/cache/clear',
        0, 0, NULL, '清空所有缓存', 1, '2026-02-04 15:25:06', 1, '2026-02-04 15:25:27', NULL),
       (67, 1, '在线用户', 54, 2, 4, '/system/online/index', 'system/online/index', 0, 0, 1, 1, NULL,
        'ionicons5:ApertureOutline', 'GET', NULL, 0, 0, NULL, NULL, 1, '2026-02-05 12:02:09', 1, '2026-02-05 12:02:09',
        2),
       (68, 1, 'API配置管理', 44, 2, 6, '/system/apiConfig', 'system/apiConfig', 0, 0, 1, 1, 'system:apiConfig:list',
        'ionicons5:BuildSharp', NULL, NULL, 0, 0, NULL, 'API接口配置管理', 1, '2026-02-09 16:29:33', 1,
        '2026-03-16 17:28:08', NULL),
       (69, 1, 'API配置管理-分页查询', 68, 4, 1, NULL, NULL, 0, 0, 1, 1, 'system:apiConfig:page', NULL, 'GET',
        '/system/apiConfig/page', 0, 0, NULL, '分页查询API配置列表', 1, '2026-02-09 16:29:33', 1, '2026-02-09 16:31:21',
        NULL),
       (70, 1, 'API配置管理-列表查询', 68, 4, 2, NULL, NULL, 0, 0, 1, 1, 'system:apiConfig:list', NULL, 'GET',
        '/system/apiConfig/list', 0, 0, NULL, '查询API配置列表', 1, '2026-02-09 16:29:33', 1, '2026-02-09 16:31:21',
        NULL),
       (71, 1, 'API配置管理-查询详情', 68, 4, 3, NULL, NULL, 0, 0, 1, 1, 'system:apiConfig:query', NULL, 'GET',
        '/system/apiConfig/getById', 0, 0, NULL, '根据ID查询API配置详情', 1, '2026-02-09 16:29:33', 1,
        '2026-02-09 16:31:21', NULL),
       (72, 1, 'API配置管理-新增', 68, 4, 4, NULL, NULL, 0, 0, 1, 1, 'system:apiConfig:add', NULL, 'POST',
        '/system/apiConfig/add', 0, 0, NULL, '新增API配置', 1, '2026-02-09 16:29:33', 1, '2026-02-09 16:31:21', NULL);
INSERT INTO forge_admin_new.sys_resource (id, tenant_id, resource_name, parent_id, resource_type, sort, `path`,
                                          component, is_external, is_public, menu_status, visible, perms, icon,
                                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by,
                                          create_time, update_by, update_time, create_dept)
VALUES (73, 1, 'API配置管理-修改', 68, 4, 5, NULL, NULL, 0, 0, 1, 1, 'system:apiConfig:edit', NULL, 'POST',
        '/system/apiConfig/edit', 0, 0, NULL, '修改API配置', 1, '2026-02-09 16:29:33', 1, '2026-02-09 16:31:21', NULL),
       (74, 1, 'API配置管理-删除', 68, 4, 6, NULL, NULL, 0, 0, 1, 1, 'system:apiConfig:remove', NULL, 'POST',
        '/system/apiConfig/remove', 0, 0, NULL, '删除API配置', 1, '2026-02-09 16:29:33', 1, '2026-02-09 16:31:21',
        NULL),
       (76, 1, 'API配置管理-刷新缓存', 68, 4, 8, NULL, NULL, 0, 0, 1, 1, 'system:apiConfig:refresh', NULL, 'POST',
        '/apiConfig/refresh', 0, 0, NULL, '刷新API配置缓存', 1, '2026-02-09 16:29:57', 1, '2026-02-09 16:31:21', NULL),
       (77, 1, '系统配置', 1, 2, 11, '/system/config-center', 'system/config-center', 0, 0, 1, 1, NULL,
        'ionicons5:ConstructOutline', 'GET', NULL, 0, 0, NULL, NULL, 1, '2026-02-26 09:49:55', 1, '2026-02-26 09:49:55',
        2),
       (78, 1, '服务监控', 54, 2, 0, '/system/monitor', 'system/monitor', 0, 0, 1, 1, NULL, 'ionicons5:Earth', 'GET',
        NULL, 0, 0, NULL, NULL, 1, '2026-03-02 14:28:26', 1, '2026-03-02 14:28:26', 2),
       (79, 1, '流程管理', 0, 1, 50, '/flow', NULL, 0, 0, 1, 1, NULL, 'ionicons5:Flower', NULL, NULL, 0, 0, NULL,
        '流程管理目录', NULL, '2026-03-15 18:32:12', 1, '2026-03-15 20:14:29', NULL),
       (80, 1, '我的待办', 9045, 2, 1, '/flow/todo', '/flow/todo', 0, 0, 1, 1, NULL, 'ionicons5:LogoMastodon', NULL,
        NULL, 0, 0, NULL, '我的待办任务', NULL, '2026-03-15 18:32:12', 1, '2026-03-26 17:24:18', NULL),
       (81, 1, '我的已办', 9045, 2, 2, '/flow/done', '/flow/done', 0, 0, 1, 1, NULL, 'ionicons5:CheckmarkDoneCircle',
        NULL, NULL, 0, 0, NULL, '我的已办任务', NULL, '2026-03-15 18:32:12', 1, '2026-03-26 17:24:24', NULL),
       (82, 1, '我发起的', 9045, 2, 3, '/flow/started', '/flow/started', 0, 0, 1, 1, NULL, 'ionicons5:Send', NULL, NULL,
        0, 0, NULL, '我发起的流程', NULL, '2026-03-15 18:32:12', 1, '2026-03-26 17:24:32', NULL),
       (83, 1, '我的抄送', 9045, 2, 4, '/flow/cc', '/flow/cc', 0, 0, 1, 1, NULL, 'ionicons5:SendOutline', NULL, NULL, 0,
        0, NULL, '我的抄送', NULL, '2026-03-15 18:32:13', 1, '2026-03-26 17:24:47', NULL);
INSERT INTO forge_admin_new.sys_resource (id, tenant_id, resource_name, parent_id, resource_type, sort, `path`,
                                          component, is_external, is_public, menu_status, visible, perms, icon,
                                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by,
                                          create_time, update_by, update_time, create_dept)
VALUES (84, 1, '流程配置', 79, 1, 10, '/flow/admin', NULL, 0, 0, 1, 1, NULL, 'ionicons5:DiscOutline', NULL, NULL, 0, 0,
        NULL, '流程管理配置', NULL, '2026-03-15 18:32:13', 1, '2026-03-15 20:16:42', NULL),
       (85, 1, '流程模型', 84, 2, 1, '/flow/admin/model', 'flow/model', 0, 0, 1, 1, NULL, 'ionicons5:Create', NULL,
        NULL, 0, 0, NULL, '流程模型管理', NULL, '2026-03-15 18:32:13', 1, '2026-03-15 21:09:05', NULL),
       (86, 1, '流程分类', 84, 2, 2, '/flow/admin/category', '/flow/category', 0, 0, 1, 1, NULL,
        'ionicons5:DuplicateOutline', NULL, NULL, 0, 0, NULL, '流程分类管理', NULL, '2026-03-15 18:32:13', 1,
        '2026-03-15 20:17:10', NULL),
       (87, 1, '创建模型', 85, 3, 1, NULL, NULL, 0, 0, 1, 1, 'flow:model:create', NULL, NULL, NULL, 0, 0, NULL,
        '创建流程模型', NULL, '2026-03-15 18:32:13', NULL, '2026-03-15 18:32:39', NULL),
       (88, 1, '编辑模型', 85, 3, 2, NULL, NULL, 0, 0, 1, 1, 'flow:model:update', NULL, NULL, NULL, 0, 0, NULL,
        '编辑流程模型', NULL, '2026-03-15 18:32:13', NULL, '2026-03-15 18:32:39', NULL),
       (89, 1, '删除模型', 85, 3, 3, NULL, NULL, 0, 0, 1, 1, 'flow:model:delete', NULL, NULL, NULL, 0, 0, NULL,
        '删除流程模型', NULL, '2026-03-15 18:32:13', NULL, '2026-03-15 18:32:39', NULL),
       (90, 1, '部署模型', 85, 3, 4, NULL, NULL, 0, 0, 1, 1, 'flow:model:deploy', NULL, NULL, NULL, 0, 0, NULL,
        '部署流程模型', NULL, '2026-03-15 18:32:13', NULL, '2026-03-15 18:32:39', NULL),
       (91, 1, '创建分类', 86, 3, 1, NULL, NULL, 0, 0, 1, 1, 'flow:category:create', NULL, NULL, NULL, 0, 0, NULL,
        '创建流程分类', NULL, '2026-03-15 18:32:13', NULL, '2026-03-15 18:32:39', NULL),
       (92, 1, '编辑分类', 86, 3, 2, NULL, NULL, 0, 0, 1, 1, 'flow:category:update', NULL, NULL, NULL, 0, 0, NULL,
        '编辑流程分类', NULL, '2026-03-15 18:32:13', NULL, '2026-03-15 18:32:39', NULL),
       (93, 1, '删除分类', 86, 3, 3, NULL, NULL, 0, 0, 1, 1, 'flow:category:delete', NULL, NULL, NULL, 0, 0, NULL,
        '删除流程分类', NULL, '2026-03-15 18:32:13', NULL, '2026-03-15 18:32:39', NULL);
INSERT INTO forge_admin_new.sys_resource (id, tenant_id, resource_name, parent_id, resource_type, sort, `path`,
                                          component, is_external, is_public, menu_status, visible, perms, icon,
                                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by,
                                          create_time, update_by, update_time, create_dept)
VALUES (94, 1, '待办任务查询', 85, 4, 10, NULL, NULL, 0, 0, 1, 1, 'flow:task:todo', NULL, 'GET', '/api/flow/task/todo',
        0, 0, NULL, '查询待办任务', NULL, '2026-03-15 18:32:13', NULL, '2026-03-15 18:32:39', NULL),
       (95, 1, '已办任务查询', 85, 4, 11, NULL, NULL, 0, 0, 1, 1, 'flow:task:done', NULL, 'GET', '/api/flow/task/done',
        0, 0, NULL, '查询已办任务', NULL, '2026-03-15 18:32:13', NULL, '2026-03-15 18:32:39', NULL),
       (96, 1, '发起的流程查询', 85, 4, 12, NULL, NULL, 0, 0, 1, 1, 'flow:task:started', NULL, 'GET',
        '/api/flow/task/started', 0, 0, NULL, '查询发起的流程', NULL, '2026-03-15 18:32:13', NULL,
        '2026-03-15 18:32:39', NULL),
       (97, 1, '任务签收', 85, 4, 13, NULL, NULL, 0, 0, 1, 1, 'flow:task:claim', NULL, 'POST', '/api/flow/task/claim',
        0, 0, NULL, '签收任务', NULL, '2026-03-15 18:32:13', NULL, '2026-03-15 18:32:39', NULL),
       (98, 1, '任务审批', 85, 4, 14, NULL, NULL, 0, 0, 1, 1, 'flow:task:approve', NULL, 'POST',
        '/api/flow/task/approve', 0, 0, NULL, '审批任务', NULL, '2026-03-15 18:32:13', NULL, '2026-03-15 18:32:39',
        NULL),
       (99, 1, '任务驳回', 85, 4, 15, NULL, NULL, 0, 0, 1, 1, 'flow:task:reject', NULL, 'POST', '/api/flow/task/reject',
        0, 0, NULL, '驳回任务', NULL, '2026-03-15 18:32:13', NULL, '2026-03-15 18:32:39', NULL),
       (100, 1, '任务催办', 85, 4, 16, NULL, NULL, 0, 0, 1, 1, 'flow:task:remind', NULL, 'POST',
        '/api/flow/task/remind', 0, 0, NULL, '催办任务', NULL, '2026-03-15 18:32:13', NULL, '2026-03-15 18:32:40',
        NULL),
       (101, 1, '流程设计', 79, 2, 3, '/flow/design', '/flow/design', 0, 0, 1, 0, NULL, 'mdi:pencil-ruler', NULL, NULL,
        0, 0, NULL, '流程设计器', NULL, '2026-03-16 16:45:04', NULL, '2026-03-16 16:45:09', NULL),
       (900, 1, '请假管理', 9045, 1, 90, '/leave', NULL, 0, 0, 1, 1, NULL, 'ionicons5:BarChartSharp', NULL, NULL, 0, 0,
        NULL, '请假管理目录', NULL, '2026-03-22 17:58:08', 1, '2026-03-26 17:25:13', NULL),
       (901, 1, '请假申请', 900, 2, 1, '/leave/apply', '/leave/apply', 0, 0, 1, 1, 'leave:apply:view',
        'ionicons5:CogSharp', NULL, NULL, 0, 0, NULL, '请假申请页面', NULL, '2026-03-22 17:58:08', 1,
        '2026-03-22 18:02:20', NULL);
INSERT INTO forge_admin_new.sys_resource (id, tenant_id, resource_name, parent_id, resource_type, sort, `path`,
                                          component, is_external, is_public, menu_status, visible, perms, icon,
                                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by,
                                          create_time, update_by, update_time, create_dept)
VALUES (902, 1, '我的请假', 900, 2, 2, '/leave/list', '/leave/list', 0, 0, 1, 1, 'leave:list:view',
        'ionicons5:BarbellSharp', NULL, NULL, 0, 0, NULL, '我的请假列表页面', NULL, '2026-03-22 17:58:08', 1,
        '2026-03-22 18:02:05', NULL),
       (9011, 1, '提交申请', 901, 3, 1, NULL, NULL, 0, 0, 1, 1, 'leave:apply:submit', NULL, NULL, NULL, 0, 0, NULL, '',
        NULL, '2026-03-22 17:58:08', NULL, '2026-03-22 17:58:53', NULL),
       (9012, 1, '保存草稿', 901, 3, 2, NULL, NULL, 0, 0, 1, 1, 'leave:apply:draft', NULL, NULL, NULL, 0, 0, NULL, '',
        NULL, '2026-03-22 17:58:08', NULL, '2026-03-22 17:58:53', NULL),
       (9021, 1, '查看详情', 902, 3, 1, NULL, NULL, 0, 0, 1, 1, 'leave:list:detail', NULL, NULL, NULL, 0, 0, NULL, '',
        NULL, '2026-03-22 17:58:08', NULL, '2026-03-22 17:58:53', NULL),
       (9022, 1, '撤销申请', 902, 3, 2, NULL, NULL, 0, 0, 1, 1, 'leave:list:cancel', NULL, NULL, NULL, 0, 0, NULL, '',
        NULL, '2026-03-22 17:58:08', NULL, '2026-03-22 17:58:53', NULL),
       (9023, 1, '删除申请', 902, 3, 3, NULL, NULL, 0, 0, 1, 1, 'leave:list:delete', NULL, NULL, NULL, 0, 0, NULL, '',
        NULL, '2026-03-22 17:58:08', NULL, '2026-03-22 17:58:53', NULL),
       (9031, 1, '提交请假申请', 901, 4, 1, NULL, NULL, 0, 0, 1, 1, 'leave:apply:submit', NULL, 'POST', '/leave/submit',
        0, 0, NULL, '', NULL, '2026-03-22 17:58:08', NULL, '2026-03-22 17:58:54', NULL),
       (9032, 1, '保存草稿', 901, 4, 2, NULL, NULL, 0, 0, 1, 1, 'leave:apply:draft', NULL, 'POST', '/leave/draft', 0, 0,
        NULL, '', NULL, '2026-03-22 17:58:08', NULL, '2026-03-22 17:58:54', NULL),
       (9041, 1, '获取请假详情', 902, 4, 1, NULL, NULL, 0, 0, 1, 1, 'leave:list:detail', NULL, 'GET', '/leave/detail/*',
        0, 0, NULL, '', NULL, '2026-03-22 17:58:24', NULL, '2026-03-22 17:58:54', NULL),
       (9042, 1, '分页查询请假列表', 902, 4, 2, NULL, NULL, 0, 0, 1, 1, 'leave:list:view', NULL, 'GET', '/leave/page',
        0, 0, NULL, '', NULL, '2026-03-22 17:58:24', NULL, '2026-03-22 17:58:54', NULL);
INSERT INTO forge_admin_new.sys_resource (id, tenant_id, resource_name, parent_id, resource_type, sort, `path`,
                                          component, is_external, is_public, menu_status, visible, perms, icon,
                                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by,
                                          create_time, update_by, update_time, create_dept)
VALUES (9043, 1, '撤销申请', 902, 4, 3, NULL, NULL, 0, 0, 1, 1, 'leave:list:cancel', NULL, 'POST', '/leave/cancel/*', 0,
        0, NULL, '', NULL, '2026-03-22 17:58:24', NULL, '2026-03-22 17:58:54', NULL),
       (9044, 1, '删除申请', 902, 4, 4, NULL, NULL, 0, 0, 1, 1, 'leave:list:delete', NULL, 'DELETE', '/leave/*', 0, 0,
        NULL, '', NULL, '2026-03-22 17:58:25', NULL, '2026-03-22 17:58:54', NULL),
       (9045, 1, '个人中心', 0, 1, 8, '/system/usercenter', NULL, 0, 0, 1, 1, NULL, 'ionicons5:Accessibility', 'GET',
        NULL, 0, 0, NULL, NULL, 1, '2026-03-26 17:23:58', 1, '2026-03-26 17:23:58', 2),
       (9046, 1, '流程监控', 85, 2, 2, '/flow/monitor', 'flow/monitor', 0, 0, 0, 0, NULL, NULL, 'GET', NULL, 0, 0, NULL,
        NULL, 1, '2026-03-26 20:19:24', 1, '2026-03-26 20:19:24', 2);
INSERT INTO forge_admin_new.sys_role (id, tenant_id, role_name, role_key, data_scope, sort, role_status, is_system,
                                      remark, create_by, create_time, update_by, update_time, create_dept)
VALUES (1, 1, '超级管理员', 'admin', 1, 1, 1, 1, '超级管理员角色，拥有所有权限', NULL, '2025-11-12 17:41:18', NULL,
        '2025-11-12 17:41:18', NULL),
       (2, 1, '普通用户', 'user', 1, 2, 1, 0, '普通用户角色，只有查询权限', NULL, '2025-11-12 17:41:18', NULL,
        '2025-12-03 16:54:01', NULL),
       (3, 1, '部门管理员', 'dept_admin', 3, 3, 1, 0, '部门管理员，管理本部门数据', NULL, '2025-11-12 17:41:18', NULL,
        '2025-11-12 17:41:18', NULL),
       (4, 0, 'ceshi', '123', 2, 0, 1, 0, NULL, NULL, '2025-12-09 12:00:19', NULL, '2025-12-09 12:00:19', NULL);
INSERT INTO forge_admin_new.sys_role_resource (id, tenant_id, role_id, resource_id, create_time)
VALUES (29, 1, 3, 1, '2025-11-12 17:41:18'),
       (30, 1, 3, 2, '2025-11-12 17:41:18'),
       (31, 1, 3, 3, '2025-11-12 17:41:18'),
       (32, 1, 3, 4, '2025-11-12 17:41:18'),
       (33, 1, 3, 5, '2025-11-12 17:41:18'),
       (34, 1, 3, 14, '2025-11-12 17:41:18'),
       (35, 1, 3, 15, '2025-11-12 17:41:18'),
       (36, 1, 3, 16, '2025-11-12 17:41:18'),
       (37, 0, 4, 19, '2025-12-09 12:00:27'),
       (38, 0, 4, 20, '2025-12-09 12:00:27');
INSERT INTO forge_admin_new.sys_role_resource (id, tenant_id, role_id, resource_id, create_time)
VALUES (39, 0, 4, 21, '2025-12-09 12:00:27'),
       (40, 0, 4, 22, '2025-12-09 12:00:27'),
       (41, 0, 4, 23, '2025-12-09 12:00:27'),
       (42, 0, 4, 1, '2025-12-09 12:00:27'),
       (43, 0, 4, 2, '2025-12-09 12:00:27'),
       (44, 0, 4, 3, '2025-12-09 12:00:27'),
       (45, 0, 4, 7, '2025-12-09 12:00:27'),
       (46, 0, 4, 8, '2025-12-09 12:00:27'),
       (47, 0, 4, 4, '2025-12-09 12:00:27'),
       (48, 0, 4, 5, '2025-12-09 12:00:27');
INSERT INTO forge_admin_new.sys_role_resource (id, tenant_id, role_id, resource_id, create_time)
VALUES (49, 0, 4, 6, '2025-12-09 12:00:27'),
       (50, 0, 4, 9, '2025-12-09 12:00:27'),
       (51, 0, 4, 10, '2025-12-09 12:00:27'),
       (52, 0, 4, 11, '2025-12-09 12:00:27'),
       (53, 0, 4, 12, '2025-12-09 12:00:28'),
       (54, 0, 4, 13, '2025-12-09 12:00:28'),
       (55, 0, 4, 14, '2025-12-09 12:00:28'),
       (56, 0, 4, 15, '2025-12-09 12:00:28'),
       (57, 0, 4, 16, '2025-12-09 12:00:28'),
       (58, 0, 4, 24, '2025-12-09 12:00:28');
INSERT INTO forge_admin_new.sys_role_resource (id, tenant_id, role_id, resource_id, create_time)
VALUES (59, 0, 4, 17, '2025-12-09 12:00:28'),
       (60, 0, 4, 18, '2025-12-09 12:00:28'),
       (110, 1, 1, 3, '2025-12-09 17:32:21'),
       (111, 1, 1, 4, '2025-12-09 17:32:22'),
       (112, 1, 1, 5, '2025-12-09 17:32:22'),
       (113, 1, 1, 6, '2025-12-09 17:32:22'),
       (114, 1, 1, 7, '2025-12-09 17:32:22'),
       (115, 1, 1, 8, '2025-12-09 17:32:22'),
       (116, 1, 1, 10, '2025-12-09 17:32:22'),
       (117, 1, 1, 11, '2025-12-09 17:32:22');
INSERT INTO forge_admin_new.sys_role_resource (id, tenant_id, role_id, resource_id, create_time)
VALUES (118, 1, 1, 12, '2025-12-09 17:32:22'),
       (119, 1, 1, 13, '2025-12-09 17:32:22'),
       (120, 1, 1, 15, '2025-12-09 17:32:22'),
       (121, 1, 1, 16, '2025-12-09 17:32:22'),
       (122, 1, 1, 18, '2025-12-09 17:32:22'),
       (123, 1, 1, 24, '2025-12-09 17:32:22'),
       (124, 1, 1, 26, '2025-12-09 17:32:22'),
       (228, 0, 1, 79, '2026-03-15 18:32:13'),
       (229, 0, 1, 80, '2026-03-15 18:32:13'),
       (230, 0, 1, 81, '2026-03-15 18:32:13');
INSERT INTO forge_admin_new.sys_role_resource (id, tenant_id, role_id, resource_id, create_time)
VALUES (231, 0, 1, 82, '2026-03-15 18:32:13'),
       (232, 0, 1, 83, '2026-03-15 18:32:13'),
       (233, 0, 1, 84, '2026-03-15 18:32:13'),
       (234, 0, 1, 85, '2026-03-15 18:32:13'),
       (235, 0, 1, 86, '2026-03-15 18:32:13'),
       (236, 0, 1, 87, '2026-03-15 18:32:13'),
       (237, 0, 1, 88, '2026-03-15 18:32:13'),
       (238, 0, 1, 89, '2026-03-15 18:32:13'),
       (239, 0, 1, 90, '2026-03-15 18:32:13'),
       (240, 0, 1, 94, '2026-03-15 18:32:13');
INSERT INTO forge_admin_new.sys_role_resource (id, tenant_id, role_id, resource_id, create_time)
VALUES (241, 0, 1, 95, '2026-03-15 18:32:13'),
       (242, 0, 1, 96, '2026-03-15 18:32:13'),
       (243, 0, 1, 97, '2026-03-15 18:32:13'),
       (244, 0, 1, 98, '2026-03-15 18:32:13'),
       (245, 0, 1, 99, '2026-03-15 18:32:13'),
       (246, 0, 1, 100, '2026-03-15 18:32:13'),
       (247, 0, 1, 91, '2026-03-15 18:32:13'),
       (248, 0, 1, 92, '2026-03-15 18:32:13'),
       (249, 0, 1, 93, '2026-03-15 18:32:13'),
       (296, 1, 2, 1, '2026-03-26 17:15:21');
INSERT INTO forge_admin_new.sys_role_resource (id, tenant_id, role_id, resource_id, create_time)
VALUES (297, 1, 2, 2, '2026-03-26 17:15:21'),
       (298, 1, 2, 3, '2026-03-26 17:15:21'),
       (299, 1, 2, 4, '2026-03-26 17:15:21'),
       (300, 1, 2, 5, '2026-03-26 17:15:21'),
       (301, 1, 2, 6, '2026-03-26 17:15:21'),
       (302, 1, 2, 7, '2026-03-26 17:15:21'),
       (303, 1, 2, 8, '2026-03-26 17:15:21'),
       (304, 1, 2, 9, '2026-03-26 17:15:21'),
       (305, 1, 2, 10, '2026-03-26 17:15:21'),
       (306, 1, 2, 11, '2026-03-26 17:15:21');
INSERT INTO forge_admin_new.sys_role_resource (id, tenant_id, role_id, resource_id, create_time)
VALUES (307, 1, 2, 12, '2026-03-26 17:15:21'),
       (308, 1, 2, 13, '2026-03-26 17:15:21'),
       (309, 1, 2, 14, '2026-03-26 17:15:21'),
       (310, 1, 2, 15, '2026-03-26 17:15:22'),
       (311, 1, 2, 16, '2026-03-26 17:15:22'),
       (312, 1, 2, 17, '2026-03-26 17:15:22'),
       (313, 1, 2, 18, '2026-03-26 17:15:22'),
       (314, 1, 2, 19, '2026-03-26 17:15:22'),
       (315, 1, 2, 20, '2026-03-26 17:15:22'),
       (316, 1, 2, 21, '2026-03-26 17:15:22');
INSERT INTO forge_admin_new.sys_role_resource (id, tenant_id, role_id, resource_id, create_time)
VALUES (317, 1, 2, 24, '2026-03-26 17:15:22'),
       (318, 1, 2, 26, '2026-03-26 17:15:22'),
       (319, 1, 2, 27, '2026-03-26 17:15:22'),
       (320, 1, 2, 28, '2026-03-26 17:15:22'),
       (321, 1, 2, 29, '2026-03-26 17:15:22'),
       (322, 1, 2, 31, '2026-03-26 17:15:22'),
       (323, 1, 2, 44, '2026-03-26 17:15:22'),
       (324, 1, 2, 45, '2026-03-26 17:15:22'),
       (325, 1, 2, 46, '2026-03-26 17:15:22'),
       (326, 1, 2, 47, '2026-03-26 17:15:22');
INSERT INTO forge_admin_new.sys_role_resource (id, tenant_id, role_id, resource_id, create_time)
VALUES (327, 1, 2, 79, '2026-03-26 17:15:22'),
       (328, 1, 2, 80, '2026-03-26 17:15:22'),
       (329, 1, 2, 81, '2026-03-26 17:15:23'),
       (330, 1, 2, 82, '2026-03-26 17:15:23'),
       (331, 1, 2, 83, '2026-03-26 17:15:23'),
       (332, 1, 2, 84, '2026-03-26 17:15:23'),
       (333, 1, 2, 85, '2026-03-26 17:15:23'),
       (334, 1, 2, 87, '2026-03-26 17:15:23'),
       (335, 1, 2, 88, '2026-03-26 17:15:23'),
       (336, 1, 2, 89, '2026-03-26 17:15:23');
INSERT INTO forge_admin_new.sys_role_resource (id, tenant_id, role_id, resource_id, create_time)
VALUES (337, 1, 2, 90, '2026-03-26 17:15:23'),
       (338, 1, 2, 94, '2026-03-26 17:15:23'),
       (339, 1, 2, 95, '2026-03-26 17:15:23'),
       (340, 1, 2, 96, '2026-03-26 17:15:23'),
       (341, 1, 2, 97, '2026-03-26 17:15:23'),
       (342, 1, 2, 98, '2026-03-26 17:15:23'),
       (343, 1, 2, 99, '2026-03-26 17:15:23'),
       (344, 1, 2, 100, '2026-03-26 17:15:23'),
       (345, 1, 2, 101, '2026-03-26 17:15:23');
INSERT INTO forge_admin_new.sys_tenant (id, tenant_name, contact_person, contact_phone, user_limit, tenant_status,
                                        expire_time, tenant_desc, create_by, create_time, update_by, update_time,
                                        browser_icon, browser_title, system_name, system_logo, system_intro,
                                        copyright_info, system_layout, system_theme, theme_config, create_dept)
VALUES (1, '默认租户', '系统管理员', '13800138000', 0, 1, '2099-12-31 23:59:59', '系统默认租户', NULL,
        '2025-11-12 17:41:17', NULL, '2025-12-22 11:27:09', NULL, NULL, '基础框架', '', NULL, NULL, 'top-side-menu',
        '#4242F7',
        '{"primaryColor":"#4242F7","header":{"backgroundColor":"#1616EF","textColor":"#FFFFFF","height":"60px"},"topMenu":{"textColor":"#FFFFFF","textColorActive":"#060606","backgroundColorActive":"#ffffff","iconColor":"#FFFFFF","iconActiveColor":"#000204"},"sideMenu":{"backgroundColor":"#ffffff","textColor":"#FFFFFF","textColorActive":"#FFFFFF","backgroundColorActive":"#4B32FE","iconColor":"#040404","iconColorActive":"#F9F9F9","width":"220px","collapsedWidth":"64px"}}',
        NULL);
INSERT INTO forge_admin_new.sys_user (id, tenant_id, username, real_name, user_type, user_client, email, phone, id_card,
                                      gender, password, salt, user_status, avatar, last_login_time, last_login_ip,
                                      login_count, remark, create_dept, create_by, create_time, update_by, update_time)
VALUES (1, 1, 'admin', '超级管理员', 0, NULL, 'admin@example.com', '13800138000', NULL, 1,
        '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', NULL, 1, NULL, NULL, NULL, 0, NULL, NULL, NULL,
        '2025-11-12 17:41:17', 1, '2026-02-04 09:30:42'),
       (2, 1, 'test', '测试用户', 2, NULL, 'test@example.com', '13800138001', NULL, 1,
        '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', NULL, 1, NULL, NULL, NULL, 0, NULL, NULL, NULL,
        '2025-11-12 17:41:17', 1, '2026-02-05 13:03:12');
INSERT INTO forge_admin_new.sys_user_org (id, tenant_id, user_id, org_id, is_main, create_time)
VALUES (2, 1, 2, 2, 1, '2025-11-12 17:41:18'),
       (3, 1, 2, 3, 0, '2025-11-12 17:41:18'),
       (13, 1, 1, 2, 1, '2026-01-20 15:35:55'),
       (14, 1, 1, 3, 0, '2026-01-20 15:36:03');
INSERT INTO forge_admin_new.sys_user_post (id, tenant_id, user_id, post_id, is_main, create_time)
VALUES (1, 1, 1, 1, 1, '2025-11-12 17:41:18'),
       (2, 1, 2, 3, 1, '2025-11-12 17:41:18'),
       (3, 1, 2, 4, 0, '2025-11-12 17:41:18');
INSERT INTO forge_admin_new.sys_user_role (id, tenant_id, user_id, role_id, create_time)
VALUES (1, 1, 1, 1, '2025-11-12 17:41:18'),
       (2, 1, 2, 2, '2025-11-12 17:41:18'),
       (3, 1, 1, 2, '2025-12-09 16:56:37');
