
-- -----------------------------------------------------
-- Table `DosageUnit`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `DosageUnit` (
  `DosageUnitPID` BIGINT(15) NOT NULL AUTO_INCREMENT,
  `releaseNumber` BIGINT NULL,
  `code` INT NULL,
  `textSingular` VARCHAR(100) NULL,
  `textPlural` VARCHAR(100) NULL,
  `ValidFrom` DATETIME NOT NULL,
  `ValidTo` DATETIME NOT NULL,
  `LastReplicated` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`DosageUnitPID`),
  UNIQUE INDEX `codeValidFrom` (`ValidFrom` ASC, `code` ASC),
  INDEX `validToCode` (`ValidTo` ASC, `code` ASC))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `DosageDrug`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `DosageDrug` (
  `DosageDrugPID` BIGINT(15) NOT NULL AUTO_INCREMENT,
  `releaseNumber` BIGINT(15) NULL,
  `drugId` BIGINT(11) NULL,
  `dosageUnitCode` BIGINT(11) NULL,
  `drugName` VARCHAR(200) NULL,
  `ValidFrom` DATETIME NOT NULL,
  `ValidTo` DATETIME NOT NULL,
  `LastReplicated` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`DosageDrugPID`),
  UNIQUE INDEX `drugIdValidFrom` (`drugId` ASC, `ValidFrom` ASC),
  INDEX `validToDrugId` (`ValidTo` ASC, `drugId` ASC))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `DosageVersion`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `DosageVersion` (
  `DosageVersionPID` BIGINT(15) NOT NULL AUTO_INCREMENT,
  `daDate` DATE NULL,
  `lmsDate` DATE NULL,
  `releaseDate` DATE NULL,
  `releaseNumber` BIGINT(15) NULL,
  `ValidFrom` DATETIME NOT NULL,
  `ValidTo` DATETIME NULL,
  `LastReplicated` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`DosageVersionPID`),
  UNIQUE INDEX `releaseDateValidFrom` (`releaseDate` ASC, `ValidFrom` ASC),
  INDEX `validToReleaseDate` (`ValidTo` ASC, `releaseDate` ASC))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `DosageStructure`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `DosageStructure` (
  `DosageStructurePID` BIGINT(15) NOT NULL AUTO_INCREMENT,
  `releaseNumber` BIGINT(15) NULL,
  `code` VARCHAR(11) NULL,
  `type` VARCHAR(100) NULL,
  `simpleString` VARCHAR(100) NULL,
  `supplementaryText` VARCHAR(200) NULL,
  `xml` VARCHAR(10000) NULL,
  `shortTranslation` VARCHAR(70) NULL,
  `longTranslation` VARCHAR(10000) NULL,
  `ValidFrom` DATETIME NOT NULL,
  `ValidTo` DATETIME NOT NULL,
  `LastReplicated` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`DosageStructurePID`),
  UNIQUE INDEX `codeValidFrom` (`code` ASC, `ValidFrom` ASC),
  INDEX `validToCode` (`ValidTo` ASC, `code` ASC))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `DosageStructureRelation`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `DosageStructureRelation` (
  `DosageStructureRelationPID` BIGINT(15) NOT NULL AUTO_INCREMENT,
  `id` VARCHAR(200) NULL,
  `drugId` BIGINT(11) NULL,
  `dosageStructureCode` BIGINT(11) NULL,
  `releaseNumber` BIGINT(15) NULL,
  `ValidFrom` DATETIME NOT NULL,
  `ValidTo` DATETIME NOT NULL,
  `LastReplicated` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`DosageStructureRelationPID`),
  UNIQUE INDEX `idValidFrom` (`id` ASC, `ValidFrom` ASC),
  INDEX `validToId` (`ValidTo` ASC, `id` ASC))
ENGINE = InnoDB;

