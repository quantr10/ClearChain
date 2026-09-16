using ClearChain.Infrastructure.Data;
using Microsoft.EntityFrameworkCore.Infrastructure;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace ClearChain.Infrastructure.Migrations
{
    /// <summary>
    /// Drops four columns that no code reads or writes: organizations.googleid (left over
    /// from an abandoned Google OAuth integration), organizations.documenturl2 (the app only
    /// ever supported a single verification document), pickuprequests.licenseplate, and
    /// inventory.beneficiarycount. Down() recreates the columns but not their data.
    /// </summary>
    [DbContext(typeof(ApplicationDbContext))]
    [Migration("20260915000000_DropUnusedColumns")]
    public partial class DropUnusedColumns : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.Sql("""
                ALTER TABLE organizations  DROP COLUMN IF EXISTS googleid;
                ALTER TABLE organizations  DROP COLUMN IF EXISTS documenturl2;
                ALTER TABLE pickuprequests DROP COLUMN IF EXISTS licenseplate;
                ALTER TABLE inventory      DROP COLUMN IF EXISTS beneficiarycount;
                """);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.Sql("""
                ALTER TABLE organizations  ADD COLUMN IF NOT EXISTS googleid text;
                ALTER TABLE organizations  ADD COLUMN IF NOT EXISTS documenturl2 text;
                ALTER TABLE pickuprequests ADD COLUMN IF NOT EXISTS licenseplate text;
                ALTER TABLE inventory      ADD COLUMN IF NOT EXISTS beneficiarycount integer NOT NULL DEFAULT 0;
                """);
        }
    }
}
