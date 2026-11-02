ALTER TABLE `category` ADD COLUMN `icon` VARCHAR(255) NOT NULL default 'materialsymbols:category';#TODO Maybe use a lookup table instead
UPDATE `category` SET `icon`='materialsymbols:restaurant' WHERE `string_id`='food';
UPDATE `category` SET `icon`='materialsymbols:home' WHERE `string_id`='rent';
UPDATE `category` SET `icon`='materialsymbols:construction' WHERE `string_id`='utilities';
UPDATE `category` SET `icon`='materialsymbols:directions_car' WHERE `string_id`='transportation';
UPDATE `category` SET `icon`='materialsymbols:medical_services' WHERE `string_id`='healthcare';
UPDATE `category` SET `icon`='materialsymbols:credit_card' WHERE `string_id`='debt';
UPDATE `category` SET `icon`='materialsymbols:child_care' WHERE `string_id`='childcare';
UPDATE `category` SET `icon`='materialsymbols:household_supplies' WHERE `string_id`='household';
UPDATE `category` SET `icon`='materialsymbols:tv' WHERE `string_id`='entertainment';
UPDATE `category` SET `icon`='materialsymbols:self_care' WHERE `string_id`='self_care';
UPDATE `category` SET `icon`='materialsymbols:checkroom' WHERE `string_id`='clothing';
UPDATE `category` SET `icon`='materialsymbols:book' WHERE `string_id`='education';
UPDATE `category` SET `icon`='materialsymbols:featured_seasonal_and_gifts' WHERE `string_id`='gifts_and_donations';
UPDATE `category` SET `icon`='materialsymbols:emergency' WHERE `string_id`='emergency';
UPDATE `category` SET `icon`='materialsymbols:savings' WHERE `string_id`='savings';
UPDATE `category` SET `icon`='materialsymbols:fitness_center' WHERE `string_id`='fitness';
UPDATE `category` SET `icon`='materialsymbols:category' WHERE `string_id`='miscellaneous';