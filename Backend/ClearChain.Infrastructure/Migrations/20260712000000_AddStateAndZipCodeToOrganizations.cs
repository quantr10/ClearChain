using ClearChain.Infrastructure.Data;
using Microsoft.EntityFrameworkCore.Infrastructure;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace ClearChain.Infrastructure.Migrations
{
    /// <inheritdoc />
    [DbContext(typeof(ApplicationDbContext))]
    [Migration("20260712000000_AddStateAndZipCodeToOrganizations")]
    public partial class AddStateAndZipCodeToOrganizations : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.Sql("""
                ALTER TABLE organizations ADD COLUMN IF NOT EXISTS state text;
                ALTER TABLE organizations ADD COLUMN IF NOT EXISTS zipcode text;
                """);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "state",
                table: "organizations");

            migrationBuilder.DropColumn(
                name: "zipcode",
                table: "organizations");
        }
    }
}
