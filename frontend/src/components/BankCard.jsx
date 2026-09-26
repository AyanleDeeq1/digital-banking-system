import logo from "../assets/logo.png";
import "../style/BankCard.css";
function BankCard({firstName, lastName, lastFour,validDate, cvc2, type}) {
    return (
        <div className="bank-card">
            <div className="card-header">
                <img src={logo} alt="UrBank" />
                <span className="card-type">{type}</span>
            </div>
            <div className="card-number-row">
                <div className="card-number">
                    **** &nbsp; **** &nbsp; **** &nbsp; {lastFour}
                </div>
                <div className="card-chip"></div>
            </div>
            <div className="card-details">
                <div>
                    <span>Card Holder</span>
                    <p>{firstName} {lastName}</p>
                </div>
                <div>
                    <span>valid thru</span>
                    <p>{validDate}</p>
                </div>
                <div>
                    <span>CVC2</span>
                    <p>{cvc2}</p>
                </div>
            </div>
        </div>
    )
}

export default BankCard