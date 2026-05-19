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
    public DbSet<FoodImageAnalysis> FoodImageAnalyses { get; set; } = null!;
    public DbSet<Notification> Notifications { get; set; } = null!;
    public DbSet<Message> Messages { get; set; } = null!;
    public DbSet<Review> Reviews { get; set; } = null!;
    public DbSet<Report> Reports { get; set; } = null!;
    public DbSet<Dispute> Disputes { get; set; } = null!;
    public DbSet<SavedListing> SavedListings { get; set; } = null!;

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
        modelBuilder.Entity<FoodImageAnalysis>().ToTable("foodimageanalyses");
        modelBuilder.Entity<Notification>().ToTable("notifications");
        modelBuilder.Entity<Message>().ToTable("messages");
        modelBuilder.Entity<Review>().ToTable("reviews");
        modelBuilder.Entity<Report>().ToTable("reports");
        modelBuilder.Entity<Dispute>().ToTable("disputes");
        modelBuilder.Entity<SavedListing>().ToTable("savedlistings");
        
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

        // ── Notification ──────────────────────────────────────────────────────────
        modelBuilder.Entity<Notification>()
            .HasOne(n => n.Recipient).WithMany().HasForeignKey(n => n.RecipientId).OnDelete(DeleteBehavior.Cascade);
        modelBuilder.Entity<Notification>().HasIndex(n => n.RecipientId);
        modelBuilder.Entity<Notification>().HasIndex(n => n.IsRead);

        // ── Message ───────────────────────────────────────────────────────────────
        modelBuilder.Entity<Message>()
            .HasOne(m => m.Sender).WithMany().HasForeignKey(m => m.SenderId).OnDelete(DeleteBehavior.Restrict);
        modelBuilder.Entity<Message>()
            .HasOne(m => m.Receiver).WithMany().HasForeignKey(m => m.ReceiverId).OnDelete(DeleteBehavior.Restrict);
        modelBuilder.Entity<Message>()
            .HasOne(m => m.PickupRequest).WithMany().HasForeignKey(m => m.PickupRequestId).OnDelete(DeleteBehavior.Cascade);
        modelBuilder.Entity<Message>().HasIndex(m => m.PickupRequestId);

        // ── Review ────────────────────────────────────────────────────────────────
        modelBuilder.Entity<Review>()
            .HasOne(r => r.Reviewer).WithMany().HasForeignKey(r => r.ReviewerId).OnDelete(DeleteBehavior.Restrict);
        modelBuilder.Entity<Review>()
            .HasOne(r => r.Reviewed).WithMany().HasForeignKey(r => r.ReviewedId).OnDelete(DeleteBehavior.Restrict);
        modelBuilder.Entity<Review>()
            .HasOne(r => r.PickupRequest).WithMany().HasForeignKey(r => r.PickupRequestId).OnDelete(DeleteBehavior.Cascade);
        modelBuilder.Entity<Review>().HasIndex(r => r.ReviewedId);
        modelBuilder.Entity<Review>().HasIndex(r => new { r.PickupRequestId, r.ReviewerId }).IsUnique();

        // ── Report ────────────────────────────────────────────────────────────────
        modelBuilder.Entity<Report>()
            .HasOne(r => r.Reporter).WithMany().HasForeignKey(r => r.ReporterId).OnDelete(DeleteBehavior.Restrict);
        modelBuilder.Entity<Report>()
            .HasOne(r => r.Listing).WithMany().HasForeignKey(r => r.ListingId).OnDelete(DeleteBehavior.SetNull);

        // ── Dispute ───────────────────────────────────────────────────────────────
        modelBuilder.Entity<Dispute>()
            .HasOne(d => d.Initiator).WithMany().HasForeignKey(d => d.InitiatorId).OnDelete(DeleteBehavior.Restrict);
        modelBuilder.Entity<Dispute>()
            .HasOne(d => d.PickupRequest).WithMany().HasForeignKey(d => d.PickupRequestId).OnDelete(DeleteBehavior.Cascade);
        modelBuilder.Entity<Dispute>().HasIndex(d => d.PickupRequestId);

        // ── SavedListing ──────────────────────────────────────────────────────────
        modelBuilder.Entity<SavedListing>()
            .HasOne(s => s.Ngo).WithMany().HasForeignKey(s => s.NgoId).OnDelete(DeleteBehavior.Cascade);
        modelBuilder.Entity<SavedListing>()
            .HasOne(s => s.Listing).WithMany().HasForeignKey(s => s.ListingId).OnDelete(DeleteBehavior.Cascade);
        modelBuilder.Entity<SavedListing>().HasIndex(s => new { s.NgoId, s.ListingId }).IsUnique();

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