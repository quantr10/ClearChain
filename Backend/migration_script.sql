CREATE TABLE IF NOT EXISTS "__EFMigrationsHistory" (
    "MigrationId" character varying(150) NOT NULL,
    "ProductVersion" character varying(32) NOT NULL,
    CONSTRAINT "PK___EFMigrationsHistory" PRIMARY KEY ("MigrationId")
);

START TRANSACTION;


DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE pickuprequests DROP CONSTRAINT "FK_pickuprequests_organizations_groceryid";
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE pickuprequests DROP CONSTRAINT "FK_pickuprequests_organizations_ngoid";
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE pickuprequests ADD cancellationreason text;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE pickuprequests ADD isfragile boolean NOT NULL DEFAULT FALSE;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE pickuprequests ADD isheavy boolean NOT NULL DEFAULT FALSE;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE pickuprequests ADD licenseplate text;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE pickuprequests ADD requiresrefrigeration boolean NOT NULL DEFAULT FALSE;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE pickuprequests ADD vehicletype text;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE organizations ALTER COLUMN phone DROP NOT NULL;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE organizations ALTER COLUMN location DROP NOT NULL;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE organizations ALTER COLUMN address DROP NOT NULL;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE organizations ADD contactperson text;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE organizations ADD deletedat timestamp with time zone;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE organizations ADD description text;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE organizations ADD documentmimetype text;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE organizations ADD documenturl text;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE organizations ADD documenturl2 text;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE organizations ADD emailverificationtoken text;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE organizations ADD emailverificationtokenexpiry timestamp with time zone;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE organizations ADD emailverified boolean NOT NULL DEFAULT FALSE;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE organizations ADD failedlogincount integer NOT NULL DEFAULT 0;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE organizations ADD isdeleted boolean NOT NULL DEFAULT FALSE;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE organizations ADD latitude double precision;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE organizations ADD lockoutuntil timestamp with time zone;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE organizations ADD longitude double precision;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE organizations ADD pickupinstructions text;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE inventory ADD beneficiarycount integer NOT NULL DEFAULT 0;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE inventory ADD ismanuallyadded boolean NOT NULL DEFAULT FALSE;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE inventory ADD notes text;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE inventory ADD photourl text;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE inventory ADD sourcepickuprequestid text;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE clearancelistings ADD archivedat timestamp with time zone;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE clearancelistings ADD isarchived boolean NOT NULL DEFAULT FALSE;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE clearancelistings ADD viewcount integer NOT NULL DEFAULT 0;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    CREATE TABLE disputes (
        id uuid NOT NULL,
        pickuprequestid uuid NOT NULL,
        initiatorid uuid NOT NULL,
        reason text NOT NULL,
        ngostatement text,
        grocerystatement text,
        photoevidenceurl text,
        status text NOT NULL,
        adminresolution text,
        resolvedbyadminid uuid,
        createdat timestamp with time zone NOT NULL,
        resolvedat timestamp with time zone,
        CONSTRAINT "PK_disputes" PRIMARY KEY (id),
        CONSTRAINT "FK_disputes_organizations_initiatorid" FOREIGN KEY (initiatorid) REFERENCES organizations (id) ON DELETE RESTRICT,
        CONSTRAINT "FK_disputes_pickuprequests_pickuprequestid" FOREIGN KEY (pickuprequestid) REFERENCES pickuprequests (id) ON DELETE CASCADE
    );
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    CREATE TABLE foodimageanalyses (
        id uuid NOT NULL,
        groceryid uuid NOT NULL,
        imageurl text NOT NULL,
        detectedname text NOT NULL,
        detectedcategory text NOT NULL,
        estimatedexpirydate timestamp with time zone,
        notes text NOT NULL,
        confidence double precision NOT NULL,
        freshnessscore double precision NOT NULL,
        qualitygrade text NOT NULL,
        detecteditems text NOT NULL,
        analyzedat timestamp with time zone NOT NULL,
        createdat timestamp with time zone NOT NULL,
        CONSTRAINT "PK_foodimageanalyses" PRIMARY KEY (id),
        CONSTRAINT "FK_foodimageanalyses_organizations_groceryid" FOREIGN KEY (groceryid) REFERENCES organizations (id) ON DELETE CASCADE
    );
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    CREATE TABLE messages (
        id uuid NOT NULL,
        pickuprequestid uuid NOT NULL,
        senderid uuid NOT NULL,
        receiverid uuid NOT NULL,
        content text NOT NULL,
        isread boolean NOT NULL,
        sentat timestamp with time zone NOT NULL,
        readat timestamp with time zone,
        CONSTRAINT "PK_messages" PRIMARY KEY (id),
        CONSTRAINT "FK_messages_organizations_receiverid" FOREIGN KEY (receiverid) REFERENCES organizations (id) ON DELETE RESTRICT,
        CONSTRAINT "FK_messages_organizations_senderid" FOREIGN KEY (senderid) REFERENCES organizations (id) ON DELETE RESTRICT,
        CONSTRAINT "FK_messages_pickuprequests_pickuprequestid" FOREIGN KEY (pickuprequestid) REFERENCES pickuprequests (id) ON DELETE CASCADE
    );
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    CREATE TABLE notifications (
        id uuid NOT NULL,
        recipientid uuid NOT NULL,
        type text NOT NULL,
        title text NOT NULL,
        body text NOT NULL,
        relatedid text,
        relatedtype text,
        isread boolean NOT NULL,
        createdat timestamp with time zone NOT NULL,
        readat timestamp with time zone,
        CONSTRAINT "PK_notifications" PRIMARY KEY (id),
        CONSTRAINT "FK_notifications_organizations_recipientid" FOREIGN KEY (recipientid) REFERENCES organizations (id) ON DELETE CASCADE
    );
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    CREATE TABLE reports (
        id uuid NOT NULL,
        reporterid uuid NOT NULL,
        listingid uuid,
        reason text NOT NULL,
        details text,
        status text NOT NULL,
        adminnote text,
        reviewedbyadminid uuid,
        createdat timestamp with time zone NOT NULL,
        reviewedat timestamp with time zone,
        CONSTRAINT "PK_reports" PRIMARY KEY (id),
        CONSTRAINT "FK_reports_clearancelistings_listingid" FOREIGN KEY (listingid) REFERENCES clearancelistings (id) ON DELETE SET NULL,
        CONSTRAINT "FK_reports_organizations_reporterid" FOREIGN KEY (reporterid) REFERENCES organizations (id) ON DELETE RESTRICT
    );
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    CREATE TABLE reviews (
        id uuid NOT NULL,
        pickuprequestid uuid NOT NULL,
        reviewerid uuid NOT NULL,
        reviewedid uuid NOT NULL,
        rating integer NOT NULL,
        comment text,
        createdat timestamp with time zone NOT NULL,
        CONSTRAINT "PK_reviews" PRIMARY KEY (id),
        CONSTRAINT "FK_reviews_organizations_reviewedid" FOREIGN KEY (reviewedid) REFERENCES organizations (id) ON DELETE RESTRICT,
        CONSTRAINT "FK_reviews_organizations_reviewerid" FOREIGN KEY (reviewerid) REFERENCES organizations (id) ON DELETE RESTRICT,
        CONSTRAINT "FK_reviews_pickuprequests_pickuprequestid" FOREIGN KEY (pickuprequestid) REFERENCES pickuprequests (id) ON DELETE CASCADE
    );
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    CREATE TABLE savedlistings (
        id uuid NOT NULL,
        ngoid uuid NOT NULL,
        listingid uuid NOT NULL,
        savedat timestamp with time zone NOT NULL,
        CONSTRAINT "PK_savedlistings" PRIMARY KEY (id),
        CONSTRAINT "FK_savedlistings_clearancelistings_listingid" FOREIGN KEY (listingid) REFERENCES clearancelistings (id) ON DELETE CASCADE,
        CONSTRAINT "FK_savedlistings_organizations_ngoid" FOREIGN KEY (ngoid) REFERENCES organizations (id) ON DELETE CASCADE
    );
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    CREATE UNIQUE INDEX "IX_organizations_email" ON organizations (email);
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    CREATE INDEX "IX_clearancelistings_status" ON clearancelistings (status);
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    CREATE INDEX "IX_disputes_initiatorid" ON disputes (initiatorid);
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    CREATE INDEX "IX_disputes_pickuprequestid" ON disputes (pickuprequestid);
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    CREATE INDEX "IX_foodimageanalyses_analyzedat" ON foodimageanalyses (analyzedat);
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    CREATE INDEX "IX_foodimageanalyses_groceryid" ON foodimageanalyses (groceryid);
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    CREATE INDEX "IX_messages_pickuprequestid" ON messages (pickuprequestid);
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    CREATE INDEX "IX_messages_receiverid" ON messages (receiverid);
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    CREATE INDEX "IX_messages_senderid" ON messages (senderid);
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    CREATE INDEX "IX_notifications_isread" ON notifications (isread);
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    CREATE INDEX "IX_notifications_recipientid" ON notifications (recipientid);
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    CREATE INDEX "IX_reports_listingid" ON reports (listingid);
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    CREATE INDEX "IX_reports_reporterid" ON reports (reporterid);
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    CREATE UNIQUE INDEX "IX_reviews_pickuprequestid_reviewerid" ON reviews (pickuprequestid, reviewerid);
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    CREATE INDEX "IX_reviews_reviewedid" ON reviews (reviewedid);
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    CREATE INDEX "IX_reviews_reviewerid" ON reviews (reviewerid);
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    CREATE INDEX "IX_savedlistings_listingid" ON savedlistings (listingid);
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    CREATE UNIQUE INDEX "IX_savedlistings_ngoid_listingid" ON savedlistings (ngoid, listingid);
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE pickuprequests ADD CONSTRAINT "FK_pickuprequests_organizations_groceryid" FOREIGN KEY (groceryid) REFERENCES organizations (id) ON DELETE RESTRICT;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    ALTER TABLE pickuprequests ADD CONSTRAINT "FK_pickuprequests_organizations_ngoid" FOREIGN KEY (ngoid) REFERENCES organizations (id) ON DELETE RESTRICT;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260417163627_AddOrgDocuments') THEN
    INSERT INTO "__EFMigrationsHistory" ("MigrationId", "ProductVersion")
    VALUES ('20260417163627_AddOrgDocuments', '8.0.11');
    END IF;
END $EF$;
COMMIT;

START TRANSACTION;


DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260502024644_AddListingSnapshotToPickupRequest') THEN
    ALTER TABLE pickuprequests ADD listingexpirydate text;
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260502024644_AddListingSnapshotToPickupRequest') THEN
    ALTER TABLE pickuprequests ADD listingunit text NOT NULL DEFAULT '';
    END IF;
END $EF$;

DO $EF$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM "__EFMigrationsHistory" WHERE "MigrationId" = '20260502024644_AddListingSnapshotToPickupRequest') THEN
    INSERT INTO "__EFMigrationsHistory" ("MigrationId", "ProductVersion")
    VALUES ('20260502024644_AddListingSnapshotToPickupRequest', '8.0.11');
    END IF;
END $EF$;
COMMIT;

