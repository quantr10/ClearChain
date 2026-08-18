using Microsoft.EntityFrameworkCore.Migrations;
using Microsoft.EntityFrameworkCore.Infrastructure;
using ClearChain.Infrastructure.Data;

#nullable disable

namespace ClearChain.Infrastructure.Migrations
{
    /// <inheritdoc />
    [DbContext(typeof(ApplicationDbContext))]
    [Migration("20260624000000_AddCartAndPickupRequestItems")]
    public partial class AddCartAndPickupRequestItems : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.Sql("""
                ALTER TABLE listinggroups DROP CONSTRAINT IF EXISTS fk_listinggroups_clearancelistings_originallistingid;
                ALTER TABLE listinggroups ALTER COLUMN originallistingid DROP NOT NULL;
                ALTER TABLE listinggroups ADD COLUMN IF NOT EXISTS totalremoved numeric NOT NULL DEFAULT 0;

                CREATE TABLE IF NOT EXISTS carts (
                    id uuid NOT NULL,
                    ngoid uuid NOT NULL,
                    createdat timestamp with time zone NOT NULL,
                    updatedat timestamp with time zone NOT NULL,
                    CONSTRAINT pk_carts PRIMARY KEY (id),
                    CONSTRAINT fk_carts_organizations_ngoid FOREIGN KEY (ngoid)
                        REFERENCES organizations (id) ON DELETE CASCADE
                );

                CREATE UNIQUE INDEX IF NOT EXISTS ix_carts_ngoid ON carts (ngoid);

                CREATE TABLE IF NOT EXISTS cartitems (
                    id uuid NOT NULL,
                    cartid uuid NOT NULL,
                    listingid uuid NOT NULL,
                    groceryid uuid NOT NULL,
                    requestedquantity integer NOT NULL,
                    createdat timestamp with time zone NOT NULL,
                    updatedat timestamp with time zone NOT NULL,
                    CONSTRAINT pk_cartitems PRIMARY KEY (id),
                    CONSTRAINT fk_cartitems_carts_cartid FOREIGN KEY (cartid)
                        REFERENCES carts (id) ON DELETE CASCADE,
                    CONSTRAINT fk_cartitems_clearancelistings_listingid FOREIGN KEY (listingid)
                        REFERENCES clearancelistings (id) ON DELETE CASCADE,
                    CONSTRAINT fk_cartitems_organizations_groceryid FOREIGN KEY (groceryid)
                        REFERENCES organizations (id) ON DELETE CASCADE
                );

                CREATE UNIQUE INDEX IF NOT EXISTS ix_cartitems_cartid_listingid ON cartitems (cartid, listingid);
                CREATE INDEX IF NOT EXISTS ix_cartitems_groceryid ON cartitems (groceryid);
                CREATE INDEX IF NOT EXISTS ix_cartitems_listingid ON cartitems (listingid);

                CREATE TABLE IF NOT EXISTS pickuprequestitems (
                    id uuid NOT NULL,
                    pickuprequestid uuid NOT NULL,
                    listinggroupid uuid NULL,
                    originallistingid uuid NULL,
                    reservedlistingid uuid NULL,
                    requestedquantity integer NOT NULL,
                    listingtitle text NOT NULL,
                    listingcategory text NOT NULL,
                    listingexpirydate text NULL,
                    listingunit text NOT NULL,
                    listingphotourl text NULL,
                    createdat timestamp with time zone NOT NULL,
                    CONSTRAINT pk_pickuprequestitems PRIMARY KEY (id),
                    CONSTRAINT fk_pickuprequestitems_pickuprequests_pickuprequestid FOREIGN KEY (pickuprequestid)
                        REFERENCES pickuprequests (id) ON DELETE CASCADE,
                    CONSTRAINT fk_pickuprequestitems_listinggroups_listinggroupid FOREIGN KEY (listinggroupid)
                        REFERENCES listinggroups (id) ON DELETE RESTRICT,
                    CONSTRAINT fk_pickuprequestitems_clearancelistings_originallistingid FOREIGN KEY (originallistingid)
                        REFERENCES clearancelistings (id) ON DELETE SET NULL,
                    CONSTRAINT fk_pickuprequestitems_clearancelistings_reservedlistingid FOREIGN KEY (reservedlistingid)
                        REFERENCES clearancelistings (id) ON DELETE SET NULL
                );

                CREATE INDEX IF NOT EXISTS ix_pickuprequestitems_pickuprequestid ON pickuprequestitems (pickuprequestid);
                CREATE INDEX IF NOT EXISTS ix_pickuprequestitems_listinggroupid ON pickuprequestitems (listinggroupid);
                CREATE INDEX IF NOT EXISTS ix_pickuprequestitems_originallistingid ON pickuprequestitems (originallistingid);
                CREATE INDEX IF NOT EXISTS ix_pickuprequestitems_reservedlistingid ON pickuprequestitems (reservedlistingid);
                ALTER TABLE pickuprequestitems ADD COLUMN IF NOT EXISTS listingphotourl text NULL;
                """);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.Sql("""
                DROP TABLE IF EXISTS pickuprequestitems;
                DROP TABLE IF EXISTS cartitems;
                DROP TABLE IF EXISTS carts;
                """);
        }
    }
}
