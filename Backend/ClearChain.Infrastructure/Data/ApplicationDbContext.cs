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
    public DbSet<RefreshToken> RefreshTokens { get; set; } = null!;
    public DbSet<AuditLog> AuditLogs { get; set; } = null!;
    public DbSet<Inventory> Inventories { get; set; } = null!;
    public DbSet<ListingGroup> ListingGroups { get; set; } = null!;
    public DbSet<FCMToken> FCMTokens { get; set; } = null!;
    public DbSet<FoodImageAnalysis> FoodImageAnalyses { get; set; } = null!;  // ✅ ADD THIS

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);

        // Map to lowercase table names
        modelBuilder.Entity<Organization>().ToTable("organizations");
        modelBuilder.Entity<ClearanceListing>().ToTable("clearancelistings");
        modelBuilder.Entity<PickupRequest>().ToTable("pickuprequests");
        modelBuilder.Entity<RefreshToken>().ToTable("refreshtokens");
        modelBuilder.Entity<AuditLog>().ToTable("auditlogs");
        modelBuilder.Entity<Inventory>().ToTable("inventory");
        modelBuilder.Entity<ListingGroup>().ToTable("listinggroups");
        modelBuilder.Entity<FCMToken>().ToTable("fcmtokens");
        modelBuilder.Entity<FoodImageAnalysis>().ToTable("foodimageanalyses");  // ✅ ADD THIS
        
        // Configure ListingGroup - ClearanceListing relationship
        modelBuilder.Entity<ListingGroup>()
            .HasMany(lg => lg.ChildListings)
            .WithOne(cl => cl.Group)
            .HasForeignKey(cl => cl.GroupId)
            .OnDelete(DeleteBehavior.Cascade);
        
        // Configure FCMToken - Organization relationship
        modelBuilder.Entity<FCMToken>()
            .HasOne(f => f.Organization)
            .WithMany()
            .HasForeignKey(f => f.OrganizationId)
            .OnDelete(DeleteBehavior.Cascade);
        
        modelBuilder.Entity<FCMToken>()
            .HasIndex(f => f.OrganizationId);
        
        // ✅ ADD: Configure FoodImageAnalysis - Organization relationship
        modelBuilder.Entity<FoodImageAnalysis>()
            .HasOne(f => f.Grocery)
            .WithMany()
            .HasForeignKey(f => f.GroceryId)
            .OnDelete(DeleteBehavior.Cascade);
        
        // ✅ ADD: Indexes for FoodImageAnalysis
        modelBuilder.Entity<FoodImageAnalysis>()
            .HasIndex(f => f.GroceryId);
        
        modelBuilder.Entity<FoodImageAnalysis>()
            .HasIndex(f => f.AnalyzedAt);
        
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