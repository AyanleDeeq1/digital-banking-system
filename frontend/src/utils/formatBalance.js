const sekFormatter = new Intl.NumberFormat("sv-SE", {
    style: "currency",
    currency: "SEK",
    currencyDisplay: "code",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
});

export function formatBalance(balance) {
    if (typeof balance !== "number" || !Number.isFinite(balance)) {
        return "Balance unavailable";
    }
    return sekFormatter.format(balance);
}
