import "../style/AccountSummary.css";

function AccountSummary({ accounts }) {

    const total = accounts.length;

    const checking = accounts.filter(
        (account) => account.type === "CHECKING"
    ).length;

    const savings = accounts.filter(
        (account) => account.type === "SAVINGS"
    ).length;

    const active = accounts.filter(
        (account) => account.status === "ACTIVE"
    ).length;

    return (
        <div className="account-summary">

            <div className="summary-header">
                <div>
                    <p className="summary-eyebrow">OVERVIEW</p>
                    <h3>Account Summary</h3>
                </div>

                <span className="summary-total">
                    {total} accounts
                </span>
            </div>

            <div className="summary-stats">

                <div className="summary-stat">
                    <span>Checking</span>
                    <strong>{checking}</strong>
                </div>

                <div className="summary-stat">
                    <span>Savings</span>
                    <strong>{savings}</strong>
                </div>

                <div className="summary-stat">
                    <span>Active</span>
                    <strong>{active}</strong>
                </div>

            </div>

        </div>
    );
}

export default AccountSummary;