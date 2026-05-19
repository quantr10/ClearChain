using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace ClearChain.Infrastructure.Migrations
{
    /// <inheritdoc />
    public partial class AddOrgDocuments : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropForeignKey(
                name: "FK_pickuprequests_organizations_groceryid",
                table: "pickuprequests");

            migrationBuilder.DropForeignKey(
                name: "FK_pickuprequests_organizations_ngoid",
                table: "pickuprequests");

            migrationBuilder.AddColumn<string>(
                name: "cancellationreason",
                table: "pickuprequests",
                type: "text",
                nullable: true);

            migrationBuilder.AddColumn<bool>(
                name: "isfragile",
                table: "pickuprequests",
                type: "boolean",
                nullable: false,
                defaultValue: false);

            migrationBuilder.AddColumn<bool>(
                name: "isheavy",
                table: "pickuprequests",
                type: "boolean",
                nullable: false,
                defaultValue: false);

            migrationBuilder.AddColumn<string>(
                name: "licenseplate",
                table: "pickuprequests",
                type: "text",
                nullable: true);

            migrationBuilder.AddColumn<bool>(
                name: "requiresrefrigeration",
                table: "pickuprequests",
                type: "boolean",
                nullable: false,
                defaultValue: false);

            migrationBuilder.AddColumn<string>(
                name: "vehicletype",
                table: "pickuprequests",
                type: "text",
                nullable: true);

            migrationBuilder.AlterColumn<string>(
                name: "phone",
                table: "organizations",
                type: "text",
                nullable: true,
                oldClrType: typeof(string),
                oldType: "text");

            migrationBuilder.AlterColumn<string>(
                name: "location",
                table: "organizations",
                type: "text",
                nullable: true,
                oldClrType: typeof(string),
                oldType: "text");

            migrationBuilder.AlterColumn<string>(
                name: "address",
                table: "organizations",
                type: "text",
                nullable: true,
                oldClrType: typeof(string),
                oldType: "text");

            migrationBuilder.AddColumn<string>(
                name: "contactperson",
                table: "organizations",
                type: "text",
                nullable: true);

            migrationBuilder.AddColumn<DateTime>(
                name: "deletedat",
                table: "organizations",
                type: "timestamp with time zone",
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "description",
                table: "organizations",
                type: "text",
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "documentmimetype",
                table: "organizations",
                type: "text",
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "documenturl",
                table: "organizations",
                type: "text",
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "documenturl2",
                table: "organizations",
                type: "text",
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "emailverificationtoken",
                table: "organizations",
                type: "text",
                nullable: true);

            migrationBuilder.AddColumn<DateTime>(
                name: "emailverificationtokenexpiry",
                table: "organizations",
                type: "timestamp with time zone",
                nullable: true);

            migrationBuilder.AddColumn<bool>(
                name: "emailverified",
                table: "organizations",
                type: "boolean",
                nullable: false,
                defaultValue: false);

            migrationBuilder.AddColumn<int>(
                name: "failedlogincount",
                table: "organizations",
                type: "integer",
                nullable: false,
                defaultValue: 0);

            migrationBuilder.AddColumn<bool>(
                name: "isdeleted",
                table: "organizations",
                type: "boolean",
                nullable: false,
                defaultValue: false);

            migrationBuilder.AddColumn<double>(
                name: "latitude",
                table: "organizations",
                type: "double precision",
                nullable: true);

            migrationBuilder.AddColumn<DateTime>(
                name: "lockoutuntil",
                table: "organizations",
                type: "timestamp with time zone",
                nullable: true);

            migrationBuilder.AddColumn<double>(
                name: "longitude",
                table: "organizations",
                type: "double precision",
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "pickupinstructions",
                table: "organizations",
                type: "text",
                nullable: true);

            migrationBuilder.AddColumn<int>(
                name: "beneficiarycount",
                table: "inventory",
                type: "integer",
                nullable: false,
                defaultValue: 0);

            migrationBuilder.AddColumn<bool>(
                name: "ismanuallyadded",
                table: "inventory",
                type: "boolean",
                nullable: false,
                defaultValue: false);

            migrationBuilder.AddColumn<string>(
                name: "notes",
                table: "inventory",
                type: "text",
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "photourl",
                table: "inventory",
                type: "text",
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "sourcepickuprequestid",
                table: "inventory",
                type: "text",
                nullable: true);

            migrationBuilder.AddColumn<DateTime>(
                name: "archivedat",
                table: "clearancelistings",
                type: "timestamp with time zone",
                nullable: true);

            migrationBuilder.AddColumn<bool>(
                name: "isarchived",
                table: "clearancelistings",
                type: "boolean",
                nullable: false,
                defaultValue: false);

            migrationBuilder.AddColumn<int>(
                name: "viewcount",
                table: "clearancelistings",
                type: "integer",
                nullable: false,
                defaultValue: 0);

            migrationBuilder.CreateTable(
                name: "disputes",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    pickuprequestid = table.Column<Guid>(type: "uuid", nullable: false),
                    initiatorid = table.Column<Guid>(type: "uuid", nullable: false),
                    reason = table.Column<string>(type: "text", nullable: false),
                    ngostatement = table.Column<string>(type: "text", nullable: true),
                    grocerystatement = table.Column<string>(type: "text", nullable: true),
                    photoevidenceurl = table.Column<string>(type: "text", nullable: true),
                    status = table.Column<string>(type: "text", nullable: false),
                    adminresolution = table.Column<string>(type: "text", nullable: true),
                    resolvedbyadminid = table.Column<Guid>(type: "uuid", nullable: true),
                    createdat = table.Column<DateTime>(type: "timestamp with time zone", nullable: false),
                    resolvedat = table.Column<DateTime>(type: "timestamp with time zone", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_disputes", x => x.id);
                    table.ForeignKey(
                        name: "FK_disputes_organizations_initiatorid",
                        column: x => x.initiatorid,
                        principalTable: "organizations",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Restrict);
                    table.ForeignKey(
                        name: "FK_disputes_pickuprequests_pickuprequestid",
                        column: x => x.pickuprequestid,
                        principalTable: "pickuprequests",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateTable(
                name: "foodimageanalyses",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    groceryid = table.Column<Guid>(type: "uuid", nullable: false),
                    imageurl = table.Column<string>(type: "text", nullable: false),
                    detectedname = table.Column<string>(type: "text", nullable: false),
                    detectedcategory = table.Column<string>(type: "text", nullable: false),
                    estimatedexpirydate = table.Column<DateTime>(type: "timestamp with time zone", nullable: true),
                    notes = table.Column<string>(type: "text", nullable: false),
                    confidence = table.Column<double>(type: "double precision", nullable: false),
                    freshnessscore = table.Column<double>(type: "double precision", nullable: false),
                    qualitygrade = table.Column<string>(type: "text", nullable: false),
                    detecteditems = table.Column<string>(type: "text", nullable: false),
                    analyzedat = table.Column<DateTime>(type: "timestamp with time zone", nullable: false),
                    createdat = table.Column<DateTime>(type: "timestamp with time zone", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_foodimageanalyses", x => x.id);
                    table.ForeignKey(
                        name: "FK_foodimageanalyses_organizations_groceryid",
                        column: x => x.groceryid,
                        principalTable: "organizations",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateTable(
                name: "messages",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    pickuprequestid = table.Column<Guid>(type: "uuid", nullable: false),
                    senderid = table.Column<Guid>(type: "uuid", nullable: false),
                    receiverid = table.Column<Guid>(type: "uuid", nullable: false),
                    content = table.Column<string>(type: "text", nullable: false),
                    isread = table.Column<bool>(type: "boolean", nullable: false),
                    sentat = table.Column<DateTime>(type: "timestamp with time zone", nullable: false),
                    readat = table.Column<DateTime>(type: "timestamp with time zone", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_messages", x => x.id);
                    table.ForeignKey(
                        name: "FK_messages_organizations_receiverid",
                        column: x => x.receiverid,
                        principalTable: "organizations",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Restrict);
                    table.ForeignKey(
                        name: "FK_messages_organizations_senderid",
                        column: x => x.senderid,
                        principalTable: "organizations",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Restrict);
                    table.ForeignKey(
                        name: "FK_messages_pickuprequests_pickuprequestid",
                        column: x => x.pickuprequestid,
                        principalTable: "pickuprequests",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateTable(
                name: "notifications",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    recipientid = table.Column<Guid>(type: "uuid", nullable: false),
                    type = table.Column<string>(type: "text", nullable: false),
                    title = table.Column<string>(type: "text", nullable: false),
                    body = table.Column<string>(type: "text", nullable: false),
                    relatedid = table.Column<string>(type: "text", nullable: true),
                    relatedtype = table.Column<string>(type: "text", nullable: true),
                    isread = table.Column<bool>(type: "boolean", nullable: false),
                    createdat = table.Column<DateTime>(type: "timestamp with time zone", nullable: false),
                    readat = table.Column<DateTime>(type: "timestamp with time zone", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_notifications", x => x.id);
                    table.ForeignKey(
                        name: "FK_notifications_organizations_recipientid",
                        column: x => x.recipientid,
                        principalTable: "organizations",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateTable(
                name: "reports",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    reporterid = table.Column<Guid>(type: "uuid", nullable: false),
                    listingid = table.Column<Guid>(type: "uuid", nullable: true),
                    reason = table.Column<string>(type: "text", nullable: false),
                    details = table.Column<string>(type: "text", nullable: true),
                    status = table.Column<string>(type: "text", nullable: false),
                    adminnote = table.Column<string>(type: "text", nullable: true),
                    reviewedbyadminid = table.Column<Guid>(type: "uuid", nullable: true),
                    createdat = table.Column<DateTime>(type: "timestamp with time zone", nullable: false),
                    reviewedat = table.Column<DateTime>(type: "timestamp with time zone", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_reports", x => x.id);
                    table.ForeignKey(
                        name: "FK_reports_clearancelistings_listingid",
                        column: x => x.listingid,
                        principalTable: "clearancelistings",
                        principalColumn: "id",
                        onDelete: ReferentialAction.SetNull);
                    table.ForeignKey(
                        name: "FK_reports_organizations_reporterid",
                        column: x => x.reporterid,
                        principalTable: "organizations",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Restrict);
                });

            migrationBuilder.CreateTable(
                name: "reviews",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    pickuprequestid = table.Column<Guid>(type: "uuid", nullable: false),
                    reviewerid = table.Column<Guid>(type: "uuid", nullable: false),
                    reviewedid = table.Column<Guid>(type: "uuid", nullable: false),
                    rating = table.Column<int>(type: "integer", nullable: false),
                    comment = table.Column<string>(type: "text", nullable: true),
                    createdat = table.Column<DateTime>(type: "timestamp with time zone", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_reviews", x => x.id);
                    table.ForeignKey(
                        name: "FK_reviews_organizations_reviewedid",
                        column: x => x.reviewedid,
                        principalTable: "organizations",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Restrict);
                    table.ForeignKey(
                        name: "FK_reviews_organizations_reviewerid",
                        column: x => x.reviewerid,
                        principalTable: "organizations",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Restrict);
                    table.ForeignKey(
                        name: "FK_reviews_pickuprequests_pickuprequestid",
                        column: x => x.pickuprequestid,
                        principalTable: "pickuprequests",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateTable(
                name: "savedlistings",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    ngoid = table.Column<Guid>(type: "uuid", nullable: false),
                    listingid = table.Column<Guid>(type: "uuid", nullable: false),
                    savedat = table.Column<DateTime>(type: "timestamp with time zone", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_savedlistings", x => x.id);
                    table.ForeignKey(
                        name: "FK_savedlistings_clearancelistings_listingid",
                        column: x => x.listingid,
                        principalTable: "clearancelistings",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_savedlistings_organizations_ngoid",
                        column: x => x.ngoid,
                        principalTable: "organizations",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateIndex(
                name: "IX_organizations_email",
                table: "organizations",
                column: "email",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_clearancelistings_status",
                table: "clearancelistings",
                column: "status");

            migrationBuilder.CreateIndex(
                name: "IX_disputes_initiatorid",
                table: "disputes",
                column: "initiatorid");

            migrationBuilder.CreateIndex(
                name: "IX_disputes_pickuprequestid",
                table: "disputes",
                column: "pickuprequestid");

            migrationBuilder.CreateIndex(
                name: "IX_foodimageanalyses_analyzedat",
                table: "foodimageanalyses",
                column: "analyzedat");

            migrationBuilder.CreateIndex(
                name: "IX_foodimageanalyses_groceryid",
                table: "foodimageanalyses",
                column: "groceryid");

            migrationBuilder.CreateIndex(
                name: "IX_messages_pickuprequestid",
                table: "messages",
                column: "pickuprequestid");

            migrationBuilder.CreateIndex(
                name: "IX_messages_receiverid",
                table: "messages",
                column: "receiverid");

            migrationBuilder.CreateIndex(
                name: "IX_messages_senderid",
                table: "messages",
                column: "senderid");

            migrationBuilder.CreateIndex(
                name: "IX_notifications_isread",
                table: "notifications",
                column: "isread");

            migrationBuilder.CreateIndex(
                name: "IX_notifications_recipientid",
                table: "notifications",
                column: "recipientid");

            migrationBuilder.CreateIndex(
                name: "IX_reports_listingid",
                table: "reports",
                column: "listingid");

            migrationBuilder.CreateIndex(
                name: "IX_reports_reporterid",
                table: "reports",
                column: "reporterid");

            migrationBuilder.CreateIndex(
                name: "IX_reviews_pickuprequestid_reviewerid",
                table: "reviews",
                columns: new[] { "pickuprequestid", "reviewerid" },
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_reviews_reviewedid",
                table: "reviews",
                column: "reviewedid");

            migrationBuilder.CreateIndex(
                name: "IX_reviews_reviewerid",
                table: "reviews",
                column: "reviewerid");

            migrationBuilder.CreateIndex(
                name: "IX_savedlistings_listingid",
                table: "savedlistings",
                column: "listingid");

            migrationBuilder.CreateIndex(
                name: "IX_savedlistings_ngoid_listingid",
                table: "savedlistings",
                columns: new[] { "ngoid", "listingid" },
                unique: true);

            migrationBuilder.AddForeignKey(
                name: "FK_pickuprequests_organizations_groceryid",
                table: "pickuprequests",
                column: "groceryid",
                principalTable: "organizations",
                principalColumn: "id",
                onDelete: ReferentialAction.Restrict);

            migrationBuilder.AddForeignKey(
                name: "FK_pickuprequests_organizations_ngoid",
                table: "pickuprequests",
                column: "ngoid",
                principalTable: "organizations",
                principalColumn: "id",
                onDelete: ReferentialAction.Restrict);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropForeignKey(
                name: "FK_pickuprequests_organizations_groceryid",
                table: "pickuprequests");

            migrationBuilder.DropForeignKey(
                name: "FK_pickuprequests_organizations_ngoid",
                table: "pickuprequests");

            migrationBuilder.DropTable(
                name: "disputes");

            migrationBuilder.DropTable(
                name: "foodimageanalyses");

            migrationBuilder.DropTable(
                name: "messages");

            migrationBuilder.DropTable(
                name: "notifications");

            migrationBuilder.DropTable(
                name: "reports");

            migrationBuilder.DropTable(
                name: "reviews");

            migrationBuilder.DropTable(
                name: "savedlistings");

            migrationBuilder.DropIndex(
                name: "IX_organizations_email",
                table: "organizations");

            migrationBuilder.DropIndex(
                name: "IX_clearancelistings_status",
                table: "clearancelistings");

            migrationBuilder.DropColumn(
                name: "cancellationreason",
                table: "pickuprequests");

            migrationBuilder.DropColumn(
                name: "isfragile",
                table: "pickuprequests");

            migrationBuilder.DropColumn(
                name: "isheavy",
                table: "pickuprequests");

            migrationBuilder.DropColumn(
                name: "licenseplate",
                table: "pickuprequests");

            migrationBuilder.DropColumn(
                name: "requiresrefrigeration",
                table: "pickuprequests");

            migrationBuilder.DropColumn(
                name: "vehicletype",
                table: "pickuprequests");

            migrationBuilder.DropColumn(
                name: "contactperson",
                table: "organizations");

            migrationBuilder.DropColumn(
                name: "deletedat",
                table: "organizations");

            migrationBuilder.DropColumn(
                name: "description",
                table: "organizations");

            migrationBuilder.DropColumn(
                name: "documentmimetype",
                table: "organizations");

            migrationBuilder.DropColumn(
                name: "documenturl",
                table: "organizations");

            migrationBuilder.DropColumn(
                name: "documenturl2",
                table: "organizations");

            migrationBuilder.DropColumn(
                name: "emailverificationtoken",
                table: "organizations");

            migrationBuilder.DropColumn(
                name: "emailverificationtokenexpiry",
                table: "organizations");

            migrationBuilder.DropColumn(
                name: "emailverified",
                table: "organizations");

            migrationBuilder.DropColumn(
                name: "failedlogincount",
                table: "organizations");

            migrationBuilder.DropColumn(
                name: "isdeleted",
                table: "organizations");

            migrationBuilder.DropColumn(
                name: "latitude",
                table: "organizations");

            migrationBuilder.DropColumn(
                name: "lockoutuntil",
                table: "organizations");

            migrationBuilder.DropColumn(
                name: "longitude",
                table: "organizations");

            migrationBuilder.DropColumn(
                name: "pickupinstructions",
                table: "organizations");

            migrationBuilder.DropColumn(
                name: "beneficiarycount",
                table: "inventory");

            migrationBuilder.DropColumn(
                name: "ismanuallyadded",
                table: "inventory");

            migrationBuilder.DropColumn(
                name: "notes",
                table: "inventory");

            migrationBuilder.DropColumn(
                name: "photourl",
                table: "inventory");

            migrationBuilder.DropColumn(
                name: "sourcepickuprequestid",
                table: "inventory");

            migrationBuilder.DropColumn(
                name: "archivedat",
                table: "clearancelistings");

            migrationBuilder.DropColumn(
                name: "isarchived",
                table: "clearancelistings");

            migrationBuilder.DropColumn(
                name: "viewcount",
                table: "clearancelistings");

            migrationBuilder.AlterColumn<string>(
                name: "phone",
                table: "organizations",
                type: "text",
                nullable: false,
                defaultValue: "",
                oldClrType: typeof(string),
                oldType: "text",
                oldNullable: true);

            migrationBuilder.AlterColumn<string>(
                name: "location",
                table: "organizations",
                type: "text",
                nullable: false,
                defaultValue: "",
                oldClrType: typeof(string),
                oldType: "text",
                oldNullable: true);

            migrationBuilder.AlterColumn<string>(
                name: "address",
                table: "organizations",
                type: "text",
                nullable: false,
                defaultValue: "",
                oldClrType: typeof(string),
                oldType: "text",
                oldNullable: true);

            migrationBuilder.AddForeignKey(
                name: "FK_pickuprequests_organizations_groceryid",
                table: "pickuprequests",
                column: "groceryid",
                principalTable: "organizations",
                principalColumn: "id",
                onDelete: ReferentialAction.Cascade);

            migrationBuilder.AddForeignKey(
                name: "FK_pickuprequests_organizations_ngoid",
                table: "pickuprequests",
                column: "ngoid",
                principalTable: "organizations",
                principalColumn: "id",
                onDelete: ReferentialAction.Cascade);
        }
    }
}
