using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace ClearChain.Infrastructure.Migrations
{
    /// <inheritdoc />
    public partial class AddListingSnapshotToPickupRequest : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<string>(
                name: "listingexpirydate",
                table: "pickuprequests",
                type: "text",
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "listingunit",
                table: "pickuprequests",
                type: "text",
                nullable: false,
                defaultValue: "");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "listingexpirydate",
                table: "pickuprequests");

            migrationBuilder.DropColumn(
                name: "listingunit",
                table: "pickuprequests");
        }
    }
}
