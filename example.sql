/*
 Navicat Premium Data Transfer

 Source Server         : local
 Source Server Type    : MySQL
 Source Server Version : 80015
 Source Host           : localhost:3306
 Source Schema         : tkde_example

 Target Server Type    : MySQL
 Target Server Version : 80015
 File Encoding         : 65001

 Date: 26/06/2020 20:32:58
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for dataset_info
-- ----------------------------
DROP TABLE IF EXISTS `dataset_info`;
CREATE TABLE `dataset_info`  (
  `dataset_local_id` int(11) NULL DEFAULT NULL,
  `type_id` int(11) NULL DEFAULT NULL,
  `triple_count` int(11) NULL DEFAULT NULL,
  `max_in_degree` int(11) NULL DEFAULT NULL,
  `max_out_degree` int(11) NULL DEFAULT NULL,
  `edp_count` int(11) NULL DEFAULT NULL,
  `lp_count` int(11) NULL DEFAULT NULL,
  `distinct_edp` int(11) NULL DEFAULT NULL,
  `distinct_lp` int(11) NULL DEFAULT NULL,
  `property_count` int(11) NULL DEFAULT NULL,
  `class_count` int(11) NULL DEFAULT NULL
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for mrrs
-- ----------------------------
DROP TABLE IF EXISTS `mrrs`;
CREATE TABLE `mrrs`  (
  `dataset_local_id` int(11) NOT NULL,
  `subgraph` longtext CHARACTER SET utf8 COLLATE utf8_bin NULL,
  `id` int(11) NOT NULL,
  PRIMARY KEY (`dataset_local_id`, `id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for snippet
-- ----------------------------
DROP TABLE IF EXISTS `snippet`;
CREATE TABLE `snippet`  (
  `dataset_local_id` int(11) NULL DEFAULT NULL,
  `algorithm` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `keyword` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `snippet` varchar(4095) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for triple
-- ----------------------------
DROP TABLE IF EXISTS `triple`;
CREATE TABLE `triple`  (
  `dataset_local_id` int(11) NULL DEFAULT NULL,
  `subject` int(11) NULL DEFAULT NULL,
  `predicate` int(11) NULL DEFAULT NULL,
  `object` int(11) NULL DEFAULT NULL,
  `triple_id` int(11) NULL DEFAULT NULL
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of triple
-- ----------------------------
INSERT INTO `triple` VALUES (1, 1, 2, 3, 1);
INSERT INTO `triple` VALUES (1, 1, 4, 5, 2);
INSERT INTO `triple` VALUES (1, 6, 7, 1, 3);
INSERT INTO `triple` VALUES (1, 6, 2, 8, 4);
INSERT INTO `triple` VALUES (1, 6, 9, 1, 5);
INSERT INTO `triple` VALUES (1, 6, 2, 10, 6);
INSERT INTO `triple` VALUES (1, 11, 9, 1, 7);
INSERT INTO `triple` VALUES (1, 11, 2, 10, 8);
INSERT INTO `triple` VALUES (1, 12, 9, 1, 9);
INSERT INTO `triple` VALUES (1, 12, 2, 10, 10);
INSERT INTO `triple` VALUES (1, 13, 9, 1, 11);
INSERT INTO `triple` VALUES (1, 13, 2, 10, 12);
INSERT INTO `triple` VALUES (1, 14, 2, 3, 13);
INSERT INTO `triple` VALUES (1, 14, 4, 5, 14);
INSERT INTO `triple` VALUES (1, 15, 7, 14, 15);
INSERT INTO `triple` VALUES (1, 15, 2, 8, 16);
INSERT INTO `triple` VALUES (1, 15, 9, 14, 17);
INSERT INTO `triple` VALUES (1, 15, 2, 10, 18);
INSERT INTO `triple` VALUES (1, 16, 9, 14, 19);
INSERT INTO `triple` VALUES (1, 16, 2, 10, 20);
INSERT INTO `triple` VALUES (1, 17, 9, 14, 21);
INSERT INTO `triple` VALUES (1, 17, 2, 10, 22);
INSERT INTO `triple` VALUES (1, 18, 9, 14, 23);
INSERT INTO `triple` VALUES (1, 18, 2, 10, 24);

-- ----------------------------
-- Table structure for uri_label_id
-- ----------------------------
DROP TABLE IF EXISTS `uri_label_id`;
CREATE TABLE `uri_label_id`  (
  `dataset_local_id` int(11) NULL DEFAULT NULL,
  `is_literal` tinyint(4) NULL DEFAULT NULL,
  `uri` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `label` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `id` int(11) NULL DEFAULT NULL
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of uri_label_id
-- ----------------------------
INSERT INTO `uri_label_id` VALUES (1, 0, 'Germany', 'Germany', 1);
INSERT INTO `uri_label_id` VALUES (1, 0, 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type', 'type', 2);
INSERT INTO `uri_label_id` VALUES (1, 0, 'Country', 'Country', 3);
INSERT INTO `uri_label_id` VALUES (1, 0, 'isPartOf', 'isPartOf', 4);
INSERT INTO `uri_label_id` VALUES (1, 0, 'Europe', 'Europe', 5);
INSERT INTO `uri_label_id` VALUES (1, 0, 'Berlin', 'Berlin', 6);
INSERT INTO `uri_label_id` VALUES (1, 0, 'capitalOf', 'capitalOf', 7);
INSERT INTO `uri_label_id` VALUES (1, 0, 'Capital', 'Capital', 8);
INSERT INTO `uri_label_id` VALUES (1, 0, 'locatedIn', 'locatedIn', 9);
INSERT INTO `uri_label_id` VALUES (1, 0, 'City', 'City', 10);
INSERT INTO `uri_label_id` VALUES (1, 0, 'Augsburg', 'Augsburg', 11);
INSERT INTO `uri_label_id` VALUES (1, 0, 'Cologne', 'Cologne', 12);
INSERT INTO `uri_label_id` VALUES (1, 0, 'Munich', 'Munich', 13);
INSERT INTO `uri_label_id` VALUES (1, 0, 'UK', 'UK', 14);
INSERT INTO `uri_label_id` VALUES (1, 0, 'London', 'London', 15);
INSERT INTO `uri_label_id` VALUES (1, 0, 'Sheffield', 'Sheffield', 16);
INSERT INTO `uri_label_id` VALUES (1, 0, 'Birmingham', 'Birmingham', 17);
INSERT INTO `uri_label_id` VALUES (1, 0, 'Edinburgh', 'Edinburgh', 18);

SET FOREIGN_KEY_CHECKS = 1;
