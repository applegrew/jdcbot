--
-- Table structure for table `static_cmds`
--

DROP TABLE IF EXISTS `static_cmds`;
CREATE TABLE `static_cmds` (
  `id` int(11) NOT NULL auto_increment,
  `cmd_name` text NOT NULL,
  `cmd_output` text NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

--
-- Dumping data for table `static_cmds`
--

LOCK TABLES `static_cmds` WRITE;
INSERT INTO `static_cmds` VALUES (1,'+help','Output some help');
UNLOCK TABLES;

