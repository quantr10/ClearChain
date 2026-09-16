namespace ClearChain.API.Services;

/// <summary>
/// Listings are measured in kg, g, L, mL, pieces, boxes or bags. Summing those numbers
/// together and calling the result "kg" overstates weight - 3 boxes is not 3 kg - so
/// quantities are split into the three scales that can honestly be added up.
/// </summary>
public static class QuantityUnits
{
    public readonly record struct Totals(double Kg, double Litres, int Units)
    {
        public static Totals Empty => new(0, 0, 0);

        public Totals Add(string? unit, int quantity)
        {
            if (quantity <= 0) return this;

            return (unit ?? string.Empty).Trim().ToLowerInvariant() switch
            {
                "kg" or "kgs" or "kilogram" or "kilograms" => this with { Kg = Kg + quantity },
                "g" or "gram" or "grams"                   => this with { Kg = Kg + quantity / 1000.0 },
                "l" or "litre" or "litres" or "liter" or "liters" => this with { Litres = Litres + quantity },
                "ml" or "millilitre" or "millilitres"      => this with { Litres = Litres + quantity / 1000.0 },
                _                                          => this with { Units = Units + quantity }
            };
        }
    }

    /// <summary>Meals one kilogram of rescued food is generally reckoned to provide.</summary>
    public const double MealsPerKg = 2.4;

    /// <summary>Kilograms of CO2e avoided per kilogram kept out of landfill.</summary>
    public const double Co2PerKg = 2.5;

    public static Totals Sum(IEnumerable<(string? Unit, int Quantity)> quantities)
    {
        var totals = Totals.Empty;
        foreach (var (unit, quantity) in quantities)
            totals = totals.Add(unit, quantity);
        return totals;
    }
}
