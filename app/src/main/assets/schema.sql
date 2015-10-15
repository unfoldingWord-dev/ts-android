-- ---
-- Globals
-- ---

-- SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";
-- SET FOREIGN_KEY_CHECKS=0;

-- ---
-- Table 'translation_note'
-- 
-- ---

DROP TABLE IF EXISTS `translation_note`;
    
CREATE TABLE `translation_note` (
  `id` INTEGER NOT NULL AUTO_INCREMENT,
  `frame_id` INTEGER NOT NULL,
  `title` TEXT NOT NULL,
  `body` TEXT NOT NULL,
  PRIMARY KEY (`id`),
  KEY (`frame_id`)
);

-- ---
-- Table 'project'
-- 
-- ---

DROP TABLE IF EXISTS `project`;
    
CREATE TABLE `project` (
  `id` INTEGER NOT NULL AUTO_INCREMENT,
  `slug` TEXT NOT NULL,
  `sort` INTEGER NOT NULL DEFAULT 0,
  `modified_at` INTEGER NOT NULL,
  `source_language_catalog_url` TEXT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY (`slug`)
);

-- ---
-- Table 'resource'
-- 
-- ---

DROP TABLE IF EXISTS `resource`;
    
CREATE TABLE `resource` (
  `id` INTEGER NOT NULL AUTO_INCREMENT,
  `slug` TEXT NOT NULL,
  `source_language_id` INTEGER NOT NULL,
  `name` TEXT NOT NULL,
  `checking_level` INTEGER NOT NULL,
  `version` TEXT NOT NULL,
  `modified_at` INTEGER NOT NULL,
  `source_catalog_url` TEXT NOT NULL,
  `translation_notes_catalog_url` TEXT NULL DEFAULT NULL,
  `translation_words_catalog_url` TEXT NULL DEFAULT NULL,
  `translation_word_assignments_catalog_url` TEXT NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY (`slug`, `source_language_id`),
  KEY (`checking_level`)
);

-- ---
-- Table 'target_language'
-- 
-- ---

DROP TABLE IF EXISTS `target_language`;
    
CREATE TABLE `target_language` (
  `id` INTEGER NOT NULL AUTO_INCREMENT,
  `code` TEXT NOT NULL,
  `name` TEXT NOT NULL,
  `direction` TEXT NOT NULL,
  `region` TEXT NOT NULL,
  PRIMARY KEY (`id`),
  KEY (`code`)
);

-- ---
-- Table 'translation_word'
-- 
-- ---

DROP TABLE IF EXISTS `translation_word`;
    
CREATE TABLE `translation_word` (
  `id` INTEGER NOT NULL AUTO_INCREMENT,
  `slug` TEXT NOT NULL,
  `resource_id` INTEGER NOT NULL,
  `term` TEXT NOT NULL,
  `definition_title` TEXT NOT NULL,
  `definition` TEXT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY (`slug`, `resource_id`)
);

-- ---
-- Table 'source_language'
-- 
-- ---

DROP TABLE IF EXISTS `source_language`;
    
CREATE TABLE `source_language` (
  `id` INTEGER NOT NULL AUTO_INCREMENT,
  `slug` TEXT NOT NULL,
  `project_id` INTEGER NOT NULL,
  `name` TEXT NOT NULL,
  `project_name` TEXT NOT NULL,
  `project_description` TEXT NULL DEFAULT NULL,
  `direction` TEXT NOT NULL,
  `modified_at` INTEGER(10) NOT NULL,
  `resource_catalog_url` TEXT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY (`slug`, `project_id`)
);

-- ---
-- Table 'checking_question'
-- 
-- ---

DROP TABLE IF EXISTS `checking_question`;
    
CREATE TABLE `checking_question` (
  `id` INTEGER NOT NULL AUTO_INCREMENT,
  `chapter_id` INTEGER NOT NULL,
  `question` TEXT NOT NULL,
  `answer` TEXT NOT NULL,
  PRIMARY KEY (`id`),
  KEY (`chapter_id`)
);

-- ---
-- Table 'chapter'
-- 
-- ---

DROP TABLE IF EXISTS `chapter`;
    
CREATE TABLE `chapter` (
  `id` INTEGER NOT NULL AUTO_INCREMENT,
  `slug` TEXT NOT NULL,
  `resource_id` INTEGER NOT NULL,
  `reference` TEXT NULL DEFAULT NULL,
  `title` TEXT NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY (`resource_id`, `slug`)
);

-- ---
-- Table 'frame'
-- 
-- ---

DROP TABLE IF EXISTS `frame`;
    
CREATE TABLE `frame` (
  `id` INTEGER NOT NULL AUTO_INCREMENT,
  `chapter_id` INTEGER NOT NULL,
  `slug` TEXT NOT NULL,
  `body` TEXT NOT NULL,
  `format` TEXT NULL DEFAULT NULL,
  `image_url` TEXT NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY (`chapter_id`, `slug`)
);

-- ---
-- Table 'category'
-- 
-- ---

DROP TABLE IF EXISTS `category`;
    
CREATE TABLE `category` (
  `id` INTEGER NOT NULL AUTO_INCREMENT,
  `slug` TEXT NOT NULL,
  `parent_id` INTEGER NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY (`slug`, `parent_id`)
);

-- ---
-- Table 'project__category'
-- 
-- ---

DROP TABLE IF EXISTS `project__category`;
    
CREATE TABLE `project__category` (
  `id` INTEGER NOT NULL AUTO_INCREMENT,
  `project_id` INTEGER NOT NULL,
  `category_id` INTEGER NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY (`project_id`, `category_id`)
);

-- ---
-- Table 'source_language__category'
-- 
-- ---

DROP TABLE IF EXISTS `source_language__category`;
    
CREATE TABLE `source_language__category` (
  `id` INTEGER NOT NULL AUTO_INCREMENT,
  `source_language_id` INTEGER(10) NOT NULL,
  `category_id` INTEGER NOT NULL,
  `category_name` TEXT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY (`source_language_id`, `category_id`)
);

-- ---
-- Table 'frame__checking_question'
-- 
-- ---

DROP TABLE IF EXISTS `frame__checking_question`;
    
CREATE TABLE `frame__checking_question` (
  `id` INTEGER NOT NULL AUTO_INCREMENT,
  `frame_id` INTEGER NOT NULL,
  `checking_question_id` INTEGER NOT NULL,
  PRIMARY KEY (`id`),
  KEY (`frame_id`),
  KEY (`checking_question_id`)
);

-- ---
-- Table 'translation_word_related'
-- 
-- ---

DROP TABLE IF EXISTS `translation_word_related`;
    
CREATE TABLE `translation_word_related` (
  `id` INTEGER NOT NULL AUTO_INCREMENT,
  `translation_word_id` INTEGER NOT NULL,
  `related_translation_word_id` INTEGER NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY (`related_translation_word_id`, `translation_word_id`)
);

-- ---
-- Table 'translation_word_example'
-- 
-- ---

DROP TABLE IF EXISTS `translation_word_example`;
    
CREATE TABLE `translation_word_example` (
  `id` INTEGER NOT NULL AUTO_INCREMENT,
  `translation_word_id` INTEGER NOT NULL,
  `frame_slug` TEXT NOT NULL,
  `chapter_slug` TEXT NOT NULL,
  `body` TEXT NOT NULL,
  PRIMARY KEY (`id`),
  KEY (`translation_word_id`),
  KEY (`frame_slug`),
  KEY (`chapter_slug`)
);

-- ---
-- Table 'frame__translation_word'
-- 
-- ---

DROP TABLE IF EXISTS `frame__translation_word`;
    
CREATE TABLE `frame__translation_word` (
  `id` INTEGER NOT NULL AUTO_INCREMENT,
  `frame_id` INTEGER NOT NULL,
  `translation_word_id` INTEGER NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY (`frame_id`, `translation_word_id`)
);

-- ---
-- Table 'meta'
-- 
-- ---

DROP TABLE IF EXISTS `meta`;
    
CREATE TABLE `meta` (
  `id` INTEGER NOT NULL AUTO_INCREMENT,
  `key` TEXT NOT NULL,
  `value` TEXT NOT NULL,
  PRIMARY KEY (`id`)
);

-- ---
-- Foreign Keys 
-- ---

ALTER TABLE `translation_note` ADD FOREIGN KEY (frame_id) REFERENCES `frame` (`id`);
ALTER TABLE `resource` ADD FOREIGN KEY (source_language_id) REFERENCES `source_language` (`id`);
ALTER TABLE `translation_word` ADD FOREIGN KEY (resource_id) REFERENCES `resource` (`id`);
ALTER TABLE `source_language` ADD FOREIGN KEY (project_id) REFERENCES `project` (`id`);
ALTER TABLE `chapter` ADD FOREIGN KEY (resource_id) REFERENCES `resource` (`id`);
ALTER TABLE `frame` ADD FOREIGN KEY (chapter_id) REFERENCES `chapter` (`id`);
ALTER TABLE `project__category` ADD FOREIGN KEY (project_id) REFERENCES `project` (`id`);
ALTER TABLE `project__category` ADD FOREIGN KEY (category_id) REFERENCES `category` (`id`);
ALTER TABLE `source_language__category` ADD FOREIGN KEY (id) REFERENCES `source_language` (`id`);
ALTER TABLE `source_language__category` ADD FOREIGN KEY (category_id) REFERENCES `category` (`id`);
ALTER TABLE `frame__checking_question` ADD FOREIGN KEY (frame_id) REFERENCES `frame` (`id`);
ALTER TABLE `frame__checking_question` ADD FOREIGN KEY (checking_question_id) REFERENCES `checking_question` (`id`);
ALTER TABLE `translation_word_related` ADD FOREIGN KEY (translation_word_id) REFERENCES `translation_word` (`id`);
ALTER TABLE `translation_word_related` ADD FOREIGN KEY (related_translation_word_id) REFERENCES `translation_word` (`id`);
ALTER TABLE `translation_word_example` ADD FOREIGN KEY (translation_word_id) REFERENCES `translation_word` (`id`);
ALTER TABLE `frame__translation_word` ADD FOREIGN KEY (frame_id) REFERENCES `frame` (`id`);
ALTER TABLE `frame__translation_word` ADD FOREIGN KEY (translation_word_id) REFERENCES `translation_word` (`id`);

-- ---
-- Table Properties
-- ---

-- ALTER TABLE `translation_note` ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
-- ALTER TABLE `project` ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
-- ALTER TABLE `resource` ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
-- ALTER TABLE `target_language` ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
-- ALTER TABLE `translation_word` ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
-- ALTER TABLE `source_language` ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
-- ALTER TABLE `checking_question` ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
-- ALTER TABLE `chapter` ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
-- ALTER TABLE `frame` ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
-- ALTER TABLE `category` ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
-- ALTER TABLE `project__category` ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
-- ALTER TABLE `source_language__category` ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
-- ALTER TABLE `frame__checking_question` ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
-- ALTER TABLE `translation_word_related` ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
-- ALTER TABLE `translation_word_example` ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
-- ALTER TABLE `frame__translation_word` ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
-- ALTER TABLE `meta` ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ---
-- Test Data
-- ---

-- INSERT INTO `translation_note` (`id`,`frame_id`,`title`,`body`) VALUES
-- ('','','','');
-- INSERT INTO `project` (`id`,`slug`,`sort`,`modified_at`,`source_language_catalog_url`) VALUES
-- ('','','','','');
-- INSERT INTO `resource` (`id`,`slug`,`source_language_id`,`name`,`checking_level`,`version`,`modified_at`,`source_catalog_url`,`translation_notes_catalog_url`,`translation_words_catalog_url`,`translation_word_assignments_catalog_url`) VALUES
-- ('','','','','','','','','','','');
-- INSERT INTO `target_language` (`id`,`code`,`name`,`direction`,`region`) VALUES
-- ('','','','','');
-- INSERT INTO `translation_word` (`id`,`slug`,`resource_id`,`term`,`definition_title`,`definition`) VALUES
-- ('','','','','','');
-- INSERT INTO `source_language` (`id`,`slug`,`project_id`,`name`,`project_name`,`project_description`,`direction`,`modified_at`,`resource_catalog_url`) VALUES
-- ('','','','','','','','','');
-- INSERT INTO `checking_question` (`id`,`chapter_id`,`question`,`answer`) VALUES
-- ('','','','');
-- INSERT INTO `chapter` (`id`,`slug`,`resource_id`,`reference`,`title`) VALUES
-- ('','','','','');
-- INSERT INTO `frame` (`id`,`chapter_id`,`slug`,`body`,`format`,`image_url`) VALUES
-- ('','','','','','');
-- INSERT INTO `category` (`id`,`slug`,`parent_id`) VALUES
-- ('','','');
-- INSERT INTO `project__category` (`id`,`project_id`,`category_id`) VALUES
-- ('','','');
-- INSERT INTO `source_language__category` (`id`,`source_language_id`,`category_id`,`category_name`) VALUES
-- ('','','','');
-- INSERT INTO `frame__checking_question` (`id`,`frame_id`,`checking_question_id`) VALUES
-- ('','','');
-- INSERT INTO `translation_word_related` (`id`,`translation_word_id`,`related_translation_word_id`) VALUES
-- ('','','');
-- INSERT INTO `translation_word_example` (`id`,`translation_word_id`,`frame_slug`,`chapter_slug`,`body`) VALUES
-- ('','','','','');
-- INSERT INTO `frame__translation_word` (`id`,`frame_id`,`translation_word_id`) VALUES
-- ('','','');
-- INSERT INTO `meta` (`id`,`key`,`value`) VALUES
-- ('','','');