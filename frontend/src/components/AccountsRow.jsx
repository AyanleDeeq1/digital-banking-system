import "../style/AccountsRow.css";

function AccountsRow({ account }) {
    return (
        <div className="account-row">

            <div className="account-row-name">
                <strong>{account.name}</strong>
                <span>{account.type}</span>
            </div>

            <div className="account-row-number">
                <span>Account number</span>
                <strong>{account.accountNumber}</strong>
            </div>

            <span
                className={`account-row-status ${account.status.toLowerCase()}`}
            >
                {account.status}
            </span>

            <button className="account-row-view">
                View account →
            </button>

        </div>
    );
}

export default AccountsRow;