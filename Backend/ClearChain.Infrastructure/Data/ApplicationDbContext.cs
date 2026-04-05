using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Storage.ValueConversion;
using ClearChain.Domain.Entities;
using ClearChain.Domain.Enums;

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
        
        // PickupRequest → Ngo
        modelBuilder.Entity<PickupRequest>()
            .HasOne(pr => pr.Ngo)
            .WithMany()
            .HasForeignKey(pr => pr.NgoId)
            .OnDelete(DeleteBehavior.Restrict);

        // PickupRequest → Grocery
        modelBuilder.Entity<PickupRequest>()
            .HasOne(pr => pr.Grocery)
            .WithMany()
            .HasForeignKey(pr => pr.GroceryId)
            .OnDelete(DeleteBehavior.Restrict);

        // Indexes
        modelBuilder.Entity<Organization>()
            .HasIndex(o => o.Email).IsUnique();

        modelBuilder.Entity<ClearanceListing>()
            .HasIndex(cl => cl.Status);

        // Enum → lowercase string converters (match existing DB values)
        var listingStatusConverter = new ValueConverter<ListingStatus, string>(
            v => v.ToString().ToLower(),
            v => Enum.Parse<ListingStatus>(v, true));

        var pickupStatusConverter = new ValueConverter<PickupRequestStatus, string>(
            v => v.ToString().ToLower(),
            v => Enum.Parse<PickupRequestStatus>(v, true));

        var inventoryStatusConverter = new ValueConverter<InventoryStatus, string>(
            v => v.ToString().ToLower(),
            v => Enum.Parse<InventoryStatus>(v, true));

        modelBuilder.Entity<ClearanceListing>()
            .Property(cl => cl.Status)
            .HasConversion(listingStatusConverter);

        modelBuilder.Entity<PickupRequest>()
            .Property(pr => pr.Status)
            .HasConversion(pickupStatusConverter);

        modelBuilder.Entity<Inventory>()
            .Property(i => i.Status)
            .HasConversion(inventoryStatusConverter);

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