import "../style/AccountCard.css";

function AccountCard({ account }) {
    return (
        <div className={`account-card ${account.accountType.toLowerCase()}`}>

            <div className="account-card-header">
                <h3>{account.name}</h3>
                <span>{account.accountType}</span>
            </div>

            <div className="account-card-number">
                <p>Account number</p>
                <strong>{account.accountNumber}</strong>
            </div>

           <div className="account-card-footer">
                <span className={`account-status ${account.status.toLowerCase()}`}>
                    {account.status}
                </span>
            </div>

        </div>
    );
}

export default AccountCard;