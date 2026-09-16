using ClearChain.Infrastructure.Data;
using Microsoft.EntityFrameworkCore.Infrastructure;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace ClearChain.Infrastructure.Migrations
{
    /// <summary>
    /// Makes pickuprequestitems the single source of truth for a request's contents.
    ///
    /// Requests created before the cart flow carried their listing inline (listingid plus
    /// the listing* snapshot columns) and had no line items, while cart requests had items
    /// but stored placeholders in those same columns - the grocery's name as the title,
    /// "Multiple" as the category. Both shapes are reconciled here: every request gets at
    /// least one line item, and the snapshot columns are recomputed as a summary of them.
    /// </summary>
    [DbContext(typeof(ApplicationDbContext))]
    [Migration("20260910000000_BackfillPickupRequestItems")]
    public partial class BackfillPickupRequestItems : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            // 1. One line item per item-less request, rebuilt from its snapshot, falling back
            //    to the listing itself where the snapshot predates those columns.
            migrationBuilder.Sql("""
                INSERT INTO pickuprequestitems (
                    id, pickuprequestid, listinggroupid, originallistingid, reservedlistingid,
                    requestedquantity, listingtitle, listingcategory, listingexpirydate,
                    listingunit, listingphotourl, createdat)
                SELECT
                    gen_random_uuid(),
                    pr.id,
                    l.groupid,
                    COALESCE(l.splitfromlistingid, l.id),
                    l.id,
                    COALESCE(pr.requestedquantity, 0),
                    COALESCE(NULLIF(btrim(pr.listingtitle), ''), l.productname, 'Unknown item'),
                    COALESCE(NULLIF(btrim(pr.listingcategory), ''), l.category, 'OTHER'),
                    COALESCE(NULLIF(btrim(pr.listingexpirydate), ''), to_char(l.expirationdate, 'YYYY-MM-DD')),
                    COALESCE(NULLIF(btrim(pr.listingunit), ''), l.unit, ''),
                    CASE
                        WHEN l.photourl IS NULL THEN NULL
                        WHEN btrim(l.photourl) LIKE '[%' THEN (regexp_match(l.photourl, '"([^"]+)"'))[1]
                        ELSE NULLIF(btrim(l.photourl), '')
                    END,
                    pr.requestedat
                FROM pickuprequests pr
                LEFT JOIN clearancelistings l ON l.id = pr.listingid
                WHERE NOT EXISTS (
                    SELECT 1 FROM pickuprequestitems i WHERE i.pickuprequestid = pr.id
                );
                """);

            // 2. Recompute every request's summary columns from its items. Mirrors
            //    PickupRequestSummary on the API side - the two must agree.
            migrationBuilder.Sql("""
                UPDATE pickuprequests pr
                SET listingtitle      = s.title,
                    listingcategory   = s.category,
                    listingunit       = s.unit,
                    listingexpirydate = s.expiry,
                    requestedquantity = s.quantity
                FROM (
                    SELECT
                        i.pickuprequestid AS request_id,
                        CASE WHEN COUNT(*) = 1
                             THEN MIN(i.listingtitle)
                             ELSE COUNT(*) || ' items' END AS title,
                        CASE WHEN COUNT(DISTINCT lower(NULLIF(btrim(i.listingcategory), ''))) = 1
                             THEN MIN(NULLIF(btrim(i.listingcategory), ''))
                             ELSE 'Multiple' END AS category,
                        CASE WHEN COUNT(DISTINCT lower(NULLIF(btrim(i.listingunit), ''))) = 1
                             THEN MIN(NULLIF(btrim(i.listingunit), ''))
                             ELSE 'items' END AS unit,
                        MIN(NULLIF(btrim(i.listingexpirydate), '')) AS expiry,
                        SUM(i.requestedquantity) AS quantity
                    FROM pickuprequestitems i
                    GROUP BY i.pickuprequestid
                ) s
                WHERE pr.id = s.request_id;
                """);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            // Removes only the line items this migration synthesized: a request that still
            // points at a single listing and holds exactly one item mirroring it. The
            // recomputed summary columns are left in place - the values they replaced were
            // placeholders, so restoring them would be restoring the defect.
            migrationBuilder.Sql("""
                DELETE FROM pickuprequestitems i
                USING pickuprequests pr
                WHERE i.pickuprequestid = pr.id
                  AND pr.listingid IS NOT NULL
                  AND i.reservedlistingid = pr.listingid
                  AND (SELECT COUNT(*) FROM pickuprequestitems x WHERE x.pickuprequestid = pr.id) = 1;
                """);
        }
    }
}
