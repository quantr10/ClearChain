using Microsoft.EntityFrameworkCore;
using ClearChain.Domain.Entities;

namespace ClearChain.Infrastructure.Data;

public class ApplicationDbContext : DbContext
{
    public ApplicationDbContext(DbContextOptions<ApplicationDbContext> options)
        : base(options)
    {
    }

    // DbSets
    public DbSet<Organization> Organizations { get; set; } = null!;
    public DbSet<ClearanceListing> ClearanceListings { get; set; } = null!;
    public DbSet<PickupRequest> PickupRequests { get; set; } = null!;
    public DbSet<DistributedItem> DistributedItems { get; set; } = null!;
    public DbSet<RefreshToken> RefreshTokens { get; set; } = null!;
    public DbSet<AuditLog> AuditLogs { get; set; } = null!;
    public DbSet<Inventory> Inventories { get; set; } = null!;  // ✅ ADD THIS

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);

        // IMPORTANT: Map to lowercase table names (PostgreSQL convention)
        modelBuilder.Entity<Organization>().ToTable("organizations");
        modelBuilder.Entity<ClearanceListing>().ToTable("clearancelistings");
        modelBuilder.Entity<PickupRequest>().ToTable("pickuprequests");
        modelBuilder.Entity<DistributedItem>().ToTable("distributeditems");
        modelBuilder.Entity<RefreshToken>().ToTable("refreshtokens");
        modelBuilder.Entity<AuditLog>().ToTable("auditlogs");
        modelBuilder.Entity<Inventory>().ToTable("inventory");  // ✅ ADD THIS

        // Configure PickupRequest - Listing many-to-many
        modelBuilder.Entity<PickupRequest>()
            .HasMany(pr => pr.Listings)
            .WithMany()
            .UsingEntity<Dictionary<string, object>>(
                "pickuprequestlistings",
                j => j.HasOne<ClearanceListing>()
                      .WithMany()
                      .HasForeignKey("listingid"),
                j => j.HasOne<PickupRequest>()
                      .WithMany()
                      .HasForeignKey("pickuprequestid")
            );

        // Map all column names to lowercase
        foreach (var entity in modelBuilder.Model.GetEntityTypes())
        {
            foreach (var property in entity.GetProperties())
            {
                property.SetColumnName(property.Name.ToLower());
            }
        }
    }
}