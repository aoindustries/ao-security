/*
 * ao-security - Best-practices security made usable.
 * Copyright (C) 2020, 2021  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of ao-security.
 *
 * ao-security is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ao-security is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ao-security.  If not, see <http://www.gnu.org/licenses/>.
 */
CREATE OR REPLACE FUNCTION "com.aoapps.security"."HashedKey.toString" (
	this "com.aoapps.security"."HashedKey"
)
RETURNS text AS $$
BEGIN
	IF this IS NULL THEN
		RETURN NULL;
	ELSIF this.algorithm IS NULL THEN
		RETURN '*';
	ELSIF this.algorithm = 'MD5' THEN
		-- MD5 is represented as hex characters of hash only
		RETURN replace(encode(this."hash", 'hex'), E'\n', '');
	ELSIF this.algorithm IN ('SHA-1', 'SHA-224', 'SHA-256', 'SHA-384', 'SHA-512') THEN
		-- These algorithms are base-64 of hash only
		RETURN replace(encode(this."hash", 'base64'), E'\n', '');
	ELSE
		-- All others use separator and explicitely list the algorithm
		RETURN '$' || this.algorithm
			|| '$' || replace(encode(this."hash", 'base64'), E'\n', '');
	END IF;
END;
$$ LANGUAGE plpgsql
IMMUTABLE
PARALLEL SAFE
RETURNS NULL ON NULL INPUT;

COMMENT ON FUNCTION "com.aoapps.security"."HashedKey.toString" ("com.aoapps.security"."HashedKey") IS
'Matches method com.aoapps.security.HashedKey.toString';

CREATE OR REPLACE FUNCTION "com.aoapps.security"."HashedKey.toString" (
	this "com.aoapps.security"."<HashedKey>"
)
RETURNS text AS $$
DECLARE
	"isValid" text;
BEGIN
	-- Validate before casting to DOMAIN to give meaningful error message
	IF this IS DISTINCT FROM NULL
	THEN
		"isValid" := "com.aoapps.security"."HashedKey.validate"(this.algorithm, this."hash");
		IF "isValid" IS NOT NULL
		THEN
			RAISE EXCEPTION '%', "isValid";
		END IF;
	END IF;
	RETURN "com.aoapps.security"."HashedKey.toString"(this::"com.aoapps.security"."HashedKey");
END;
$$ LANGUAGE plpgsql
IMMUTABLE
PARALLEL SAFE
RETURNS NULL ON NULL INPUT;

COMMENT ON FUNCTION "com.aoapps.security"."HashedKey.toString" ("com.aoapps.security"."<HashedKey>") IS
'Matches method com.aoapps.security.HashedKey.toString';