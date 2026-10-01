import "../style/AccountCard.css";
import { formatBalance } from "../utils/formatBalance.js";

function AccountCard({ account }) {
    return (
        <div className={`account-card ${account.type.toLowerCase()}`}>

            <div className="account-card-header">
                <h3>{account.name}</h3>
                <span>{account.type}</span>
            </div>

            <div className="account-card-number">
                <p>Account number</p>
                <strong>{account.accountNumber}</strong>
            </div>

            <div className="account-card-balance">
                <p>Balance</p>
                <strong>{formatBalance(account.balance)}</strong>
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
