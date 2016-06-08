-- ---
-- Table 'translation_note'
-- ---

DROP TABLE IF EXISTS `translation_note`;
    
CREATE TABLE `translation_note` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `slug` TEXT NOT NULL,
  `frame_id` INTEGER NOT NULL,
  `project_slug` TEXT NOT NULL,
  `source_language_slug` TEXT NOT NULL,
  `resource_slug` TEXT NOT NULL,
  `chapter_slug` TEXT NOT NULL,
  `frame_slug` TEXT NOT NULL,
  `title` TEXT NOT NULL,
  `body` TEXT NOT NULL,
  FOREIGN KEY (frame_id) REFERENCES `frame` (`id`) ON DELETE CASCADE
);

-- ---
-- Table 'project'
-- ---

DROP TABLE IF EXISTS `project`;
    
CREATE TABLE `project` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `slug` TEXT NOT NULL,
  `sort` INTEGER NOT NULL DEFAULT 0,
  `modified_at` INTEGER NOT NULL,
  `source_language_catalog_url` TEXT NOT NULL,
  `source_language_catalog_local_modified_at` INTEGER NOT NULL DEFAULT 0,
  `source_language_catalog_server_modified_at` INTEGER NOT NULL DEFAULT 0,
  `chunk_marker_catalog_url` TEXT NULL DEFAULT NULL,
  `chunk_marker_catalog_local_modified_at` INTEGER NOT NULL DEFAULT 0,
  `chunk_marker_catalog_server_modified_at` INTEGER NOT NULL DEFAULT 0,
  UNIQUE (`slug`)
);


-- ---
-- Table 'chunk_marker'
-- ---

DROP TABLE IF EXISTS `chunk_marker`;

CREATE TABLE `chunk_marker` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `project_id` INTEGER NOT NULL,
  `chapter_slug` TEXT NOT NULL,
  `first_verse_slug` TEXT NOT NULL,
  UNIQUE (`project_id`, 'chapter_slug', 'first_verse_slug'),
  FOREIGN KEY (project_id) REFERENCES `project` (`id`) ON DELETE CASCADE
);

-- ---
-- Table 'resource'
-- ---

DROP TABLE IF EXISTS `resource`;
    
CREATE TABLE `resource` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `slug` TEXT NOT NULL,
  `source_language_id` INTEGER NOT NULL,
  `name` TEXT NOT NULL,
  `checking_level` INTEGER NOT NULL,
  `version` TEXT NOT NULL,
  `modified_at` INTEGER NOT NULL,
  `source_catalog_url` TEXT NOT NULL,
  `source_catalog_local_modified_at` INTEGER NOT NULL DEFAULT 0,
  `source_catalog_server_modified_at` INTEGER NOT NULL DEFAULT 0,
  `translation_notes_catalog_url` TEXT NULL DEFAULT NULL,
  `translation_notes_catalog_local_modified_at` INTEGER NOT NULL DEFAULT 0,
  `translation_notes_catalog_server_modified_at` INTEGER NOT NULL DEFAULT 0,
  `translation_words_catalog_url` TEXT NULL DEFAULT NULL,
  `translation_words_catalog_local_modified_at` INTEGER NOT NULL DEFAULT 0,
  `translation_words_catalog_server_modified_at` INTEGER NOT NULL DEFAULT 0,
  `translation_word_assignments_catalog_url` TEXT NULL DEFAULT NULL,
  `translation_word_assignments_catalog_local_modified_at` INTEGER NOT NULL DEFAULT 0,
  `translation_word_assignments_catalog_server_modified_at` INTEGER NOT NULL DEFAULT 0,
  `checking_questions_catalog_url` TEXT NULL DEFAULT NULL,
  `checking_questions_catalog_local_modified_at` INTEGER NOT NULL DEFAULT 0,
  `checking_questions_catalog_server_modified_at` INTEGER NOT NULL DEFAULT 0,
  UNIQUE (`slug`, `source_language_id`),
  FOREIGN KEY (source_language_id) REFERENCES `source_language` (`id`) ON DELETE CASCADE
);

-- ---
-- Table 'resource__translation_word'
-- ---

DROP TABLE IF EXISTS `resource__translation_word`;
    
CREATE TABLE `resource__translation_word` (
  `id` INTEGER NULL PRIMARY KEY AUTOINCREMENT,
  `resource_id` INTEGER NOT NULL,
  `translation_word_id` INTEGER NOT NULL,
  UNIQUE (`resource_id`, `translation_word_id`),
  FOREIGN KEY (resource_id) REFERENCES `resource` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (translation_word_id) REFERENCES `translation_word` (`id`) ON DELETE CASCADE
);

-- ---
-- Table 'target_language'
-- ---

DROP TABLE IF EXISTS `target_language`;
    
CREATE TABLE `target_language` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `slug` TEXT NOT NULL,
  `name` TEXT NOT NULL,
  `direction` TEXT NOT NULL,
  `region` TEXT NOT NULL,
  UNIQUE (`slug`)
);

-- ---
-- Table 'temp_target_language'
-- ---

DROP TABLE IF EXISTS `temp_target_language`;

CREATE TABLE `temp_target_language` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `slug` TEXT NOT NULL,
  `name` TEXT NOT NULL,
  `direction` TEXT NOT NULL,
  `region` TEXT NOT NULL,
  UNIQUE (`slug`)
);


-- ---
-- Table 'approved_temp_target_language'
-- ---

DROP TABLE IF EXISTS `approved_temp_target_language`;

CREATE TABLE `approved_temp_target_language` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `target_language_id` INTEGER NOT NULL,
  `temp_target_language_id` INTEGER NOT NULL,
  UNIQUE (`target_language_id`, `temp_target_language_id`),
  FOREIGN KEY (target_language_id) REFERENCES `target_language` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (temp_target_language_id) REFERENCES `temp_target_language` (`id`) ON DELETE CASCADE
);

-- ---
-- Table 'translation_word'
-- ---

DROP TABLE IF EXISTS `translation_word`;
    
CREATE TABLE `translation_word` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `slug` TEXT NOT NULL,
  `catalog_hash` TEXT NOT NULL,
  `term` TEXT NOT NULL,
  `definition_title` TEXT NOT NULL,
  `definition` TEXT NOT NULL,
  UNIQUE (`slug`, `catalog_hash`)
);

-- ---
-- Table 'source_language'
-- ---

DROP TABLE IF EXISTS `source_language`;
    
CREATE TABLE `source_language` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `slug` TEXT NOT NULL,
  `project_id` INTEGER NOT NULL,
  `name` TEXT NOT NULL,
  `project_name` TEXT NOT NULL,
  `project_description` TEXT NULL DEFAULT NULL,
  `direction` TEXT NOT NULL,
  `modified_at` INTEGER(10) NOT NULL,
  `resource_catalog_url` TEXT NOT NULL,
  `resource_catalog_local_modified_at` INTEGER NOT NULL DEFAULT 0,
  `resource_catalog_server_modified_at` INTEGER NOT NULL DEFAULT 0,
  UNIQUE (`slug`, `project_id`),
  FOREIGN KEY (project_id) REFERENCES `project` (`id`) ON DELETE CASCADE
);

-- ---
-- Table 'checking_question'
-- ---

DROP TABLE IF EXISTS `checking_question`;
    
CREATE TABLE `checking_question` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `slug` TEXT NOT NULL,
  `chapter_id` INTEGER NOT NULL,
  `question` TEXT NOT NULL,
  `answer` TEXT NOT NULL,
  UNIQUE (`slug`, `chapter_id`)
);

-- ---
-- Table 'chapter'
-- ---

DROP TABLE IF EXISTS `chapter`;
    
CREATE TABLE `chapter` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `slug` TEXT NOT NULL,
  `resource_id` INTEGER NOT NULL,
  `reference` TEXT NULL DEFAULT NULL,
  `title` TEXT NULL DEFAULT NULL,
  `sort` INTEGER NOT NULL DEFAULT 0,
  UNIQUE (`resource_id`, `slug`),
  FOREIGN KEY (resource_id) REFERENCES `resource` (`id`) ON DELETE CASCADE
);

-- ---
-- Table 'frame'
-- ---

DROP TABLE IF EXISTS `frame`;
    
CREATE TABLE `frame` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `slug` TEXT NOT NULL,
  `chapter_id` INTEGER NOT NULL,
  `body` TEXT NOT NULL,
  `format` TEXT NULL DEFAULT NULL,
  `image_url` TEXT NULL DEFAULT NULL,
  `sort` INTEGER NOT NULL DEFAULT 0,
  UNIQUE (`chapter_id`, `slug`),
  FOREIGN KEY (chapter_id) REFERENCES `chapter` (`id`) ON DELETE CASCADE
);

-- ---
-- Table 'category'
-- ---

DROP TABLE IF EXISTS `category`;
    
CREATE TABLE `category` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `slug` TEXT NOT NULL,
  `parent_id` INTEGER NOT NULL,
  UNIQUE (`slug`, `parent_id`)
);

-- ---
-- Table 'project__category'
-- ---

DROP TABLE IF EXISTS `project__category`;
    
CREATE TABLE `project__category` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `project_id` INTEGER NOT NULL,
  `category_id` INTEGER NOT NULL,
  UNIQUE (`project_id`, `category_id`),
  FOREIGN KEY (project_id) REFERENCES `project` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (category_id) REFERENCES `category` (`id`) ON DELETE CASCADE
);

-- ---
-- Table 'source_language__category'
-- ---

DROP TABLE IF EXISTS `source_language__category`;
    
CREATE TABLE `source_language__category` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `source_language_id` INTEGER(10) NOT NULL,
  `category_id` INTEGER NOT NULL,
  `category_name` TEXT NOT NULL,
  UNIQUE (`source_language_id`, `category_id`),
  FOREIGN KEY (source_language_id) REFERENCES `source_language` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (category_id) REFERENCES `category` (`id`) ON DELETE CASCADE
);

-- ---
-- Table 'frame__checking_question'
-- ---

DROP TABLE IF EXISTS `frame__checking_question`;

CREATE TABLE `frame__checking_question` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `frame_id` INTEGER NOT NULL,
  `checking_question_id` INTEGER NOT NULL,
  `project_slug` TEXT NOT NULL,
  `source_language_slug` TEXT NOT NULL,
  `resource_slug` TEXT NOT NULL,
  `chapter_slug` TEXT NOT NULL,
  `frame_slug` TEXT NOT NULL,
  FOREIGN KEY (frame_id) REFERENCES `frame` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (checking_question_id) REFERENCES `checking_question` (`id`) ON DELETE CASCADE
);

-- ---
-- Table 'translation_word_related'
-- ---

DROP TABLE IF EXISTS `translation_word_related`;
    
CREATE TABLE `translation_word_related` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `translation_word_id` INTEGER NOT NULL,
  `slug` TEXT NOT NULL,
  UNIQUE (`slug`, `translation_word_id`),
  FOREIGN KEY (translation_word_id) REFERENCES `translation_word` (`id`) ON DELETE CASCADE
);

-- ---
-- Table 'translation_word_example'
-- ---

DROP TABLE IF EXISTS `translation_word_example`;
    
CREATE TABLE `translation_word_example` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `translation_word_id` INTEGER NOT NULL,
  `frame_slug` TEXT NOT NULL,
  `chapter_slug` TEXT NOT NULL,
  `body` TEXT NOT NULL,
  UNIQUE (`translation_word_id`, `chapter_slug`, `frame_slug`)
  FOREIGN KEY (translation_word_id) REFERENCES `translation_word` (`id`) ON DELETE CASCADE
);

-- ---
-- Table 'translation_word_alias'
-- ---

DROP TABLE IF EXISTS `translation_word_alias`;

CREATE TABLE `translation_word_alias` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `translation_word_id` INTEGER NOT NULL,
  `term` TEXT NOT NULL,
  UNIQUE (`term`, `translation_word_id`),
  FOREIGN KEY (translation_word_id) REFERENCES `translation_word` (`id`) ON DELETE CASCADE
);

-- ---
-- Table 'frame__translation_word'
-- ---

DROP TABLE IF EXISTS `frame__translation_word`;
    
CREATE TABLE `frame__translation_word` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `frame_id` INTEGER NOT NULL,
  `translation_word_id` INTEGER NOT NULL,
  `project_slug` TEXT NOT NULL,
  `source_language_slug` TEXT NOT NULL,
  `resource_slug` TEXT NOT NULL,
  `chapter_slug` TEXT NOT NULL,
  `frame_slug` TEXT NOT NULL,
  UNIQUE (`frame_id`, `translation_word_id`),
  FOREIGN KEY (frame_id) REFERENCES `frame` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (translation_word_id) REFERENCES `translation_word` (`id`) ON DELETE CASCADE
);

-- ---
-- Table 'meta'
-- ---

DROP TABLE IF EXISTS `meta`;
    
CREATE TABLE `meta` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `key` TEXT NOT NULL,
  `value` TEXT NOT NULL
);


-- ---
-- Table 'translation_academy_volume'
--
-- ---

DROP TABLE IF EXISTS `translation_academy_volume`;

CREATE TABLE `translation_academy_volume` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `slug` TEXT NOT NULL,
  `catalog_hash` TEXT NOT NULL,
  `title` TEXT NOT NULL,
  UNIQUE (`slug`, `catalog_hash`)
);

-- ---
-- Table 'translation_academy_manual'
--
-- ---

DROP TABLE IF EXISTS `translation_academy_manual`;

CREATE TABLE `translation_academy_manual` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `slug` TEXT NOT NULL,
  `translation_academy_volume_id` INTEGER NOT NULL,
  `title` TEXT NOT NULL,
  UNIQUE (`slug`, `translation_academy_volume_id`),
  FOREIGN KEY (translation_academy_volume_id) REFERENCES `translation_academy_volume` (`id`) ON DELETE CASCADE
);

-- ---
-- Table 'translation_academy_article'
--
-- ---

DROP TABLE IF EXISTS `translation_academy_article`;

CREATE TABLE `translation_academy_article` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `slug` TEXT NOT NULL,
  `translation_academy_manual_id` INTEGER NOT NULL,
  `title` TEXT NOT NULL,
  `text` TEXT NOT NULL,
  `reference` TEXT NOT NULL,
  UNIQUE (`slug`, `translation_academy_manual_id`),
  FOREIGN KEY (translation_academy_manual_id) REFERENCES `translation_academy_manual` (`id`) ON DELETE CASCADE
);

-- ---
-- Table 'resource__translation_academy_volume'
--
-- ---

DROP TABLE IF EXISTS `resource__translation_academy_volume`;

CREATE TABLE `resource__translation_academy_volume` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `resource_id` INTEGER NOT NULL,
  `translation_academy_volume_id` INTEGER NOT NULL,
  UNIQUE (`resource_id`, `translation_academy_volume_id`),
  FOREIGN KEY (resource_id) REFERENCES `resource` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (translation_academy_volume_id) REFERENCES `translation_academy_volume` (`id`) ON DELETE CASCADE
);


-- ---
-- Table 'new_target_language_questionnaire'
--
-- ---

DROP TABLE IF EXISTS `new_target_language_questionnaire`;

CREATE TABLE `new_target_language_questionnaire` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `questionnaire_td_id` INTEGER NOT NULL,
  `language_slug` TEXT NOT NULL,
  `language_name` TEXT NOT NULL,
  `language_direction` TEXT NOT NULL,
  UNIQUE (`questionnaire_td_id`)
);


-- ---
-- Table 'new_target_language_question'
--
-- ---

DROP TABLE IF EXISTS `new_target_language_question`;

CREATE TABLE `new_target_language_question` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `new_target_language_questionnaire_id` INTEGER NOT NULL,
  `question_td_id` INTEGER NOT NULL,
  `text` TEXT NOT NULL,
  `help` TEXT NOT NULL,
  `is_required` INTEGER NOT NULL DEFAULT 0,
  `input_type` TEXT NOT NULL,
  `sort` INTEGER NOT NULL DEFAULT 0,
  `depends_on` INTEGER DEFAULT NULL,
  UNIQUE (`question_td_id`, `new_target_language_questionnaire_id`),
  FOREIGN KEY (new_target_language_questionnaire_id) REFERENCES `new_target_language_questionnaire` (`id`) ON DELETE CASCADE
);


-- ---
-- Indexes
-- ---

CREATE INDEX `translation_note_frame_id` ON `translation_note`(`frame_id`);
CREATE INDEX `source_language__category_category_id` ON `source_language__category`(`category_id`);
CREATE INDEX `resource_checking_level` ON `resource`(`checking_level`);
CREATE INDEX `target_language_slug` ON `target_language`(`slug`);
CREATE INDEX `temp_target_language_slug` ON `temp_target_language`(`slug`);

CREATE INDEX `checking_question_slug` ON `checking_question`(`slug`);
CREATE INDEX `checking_question_chapter_id` ON `checking_question`(`chapter_id`);

CREATE INDEX `frame__checking_question_frame_id` ON `frame__checking_question`(`frame_id`);
CREATE INDEX `frame__checking_question_checking_question_id` ON `frame__checking_question`(`checking_question_id`);
CREATE INDEX `frame__checking_question_project_slug` ON `frame__checking_question`(`project_slug`);
CREATE INDEX `frame__checking_question_source_language_slug` ON `frame__checking_question`(`source_language_slug`);
CREATE INDEX `frame__checking_question_resource_slug` ON `frame__checking_question`(`resource_slug`);
CREATE INDEX `frame__checking_question_chapter_slug` ON `frame__checking_question`(`chapter_slug`);
CREATE INDEX `frame__checking_question_frame_slug` ON `frame__checking_question`(`frame_slug`);

CREATE INDEX `translation_word_example_translation_word_id` ON `translation_word_example`(`translation_word_id`);
CREATE INDEX `translation_word_example_frame_slug` ON `translation_word_example`(`frame_slug`);
CREATE INDEX `translation_word_example_chapter_slug` ON `translation_word_example`(`chapter_slug`);

CREATE INDEX `frame__translation_word_project_slug` ON `frame__translation_word`(`project_slug`);
CREATE INDEX `frame__translation_word_source_language_slug` ON `frame__translation_word`(`source_language_slug`);
CREATE INDEX `frame__translation_word_resource_slug` ON `frame__translation_word`(`resource_slug`);
CREATE INDEX `frame__translation_word_chapter_slug` ON `frame__translation_word`(`chapter_slug`);
CREATE INDEX `frame__translation_word_frame_slug` ON `frame__translation_word`(`frame_slug`);

CREATE INDEX `translation_note_project_slug` ON `translation_note`(`project_slug`);
CREATE INDEX `translation_note_source_language_slug` ON `translation_note`(`source_language_slug`);
CREATE INDEX `translation_note_resource_slug` ON `translation_note`(`resource_slug`);
CREATE INDEX `translation_note_chapter_slug` ON `translation_note`(`chapter_slug`);
CREATE INDEX `translation_note_frame_slug` ON `translation_note`(`frame_slug`);
