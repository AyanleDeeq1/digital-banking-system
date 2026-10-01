import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import Header from "../components/Header.jsx";
import Footer from "../components/Footer.jsx";
import SideBar from "../components/SideBar.jsx";
import BankCard from "../components/BankCard.jsx";
import "../style/MyCard.css";

function MyCard({ customer }) {
    const [result, setResult] = useState({ state: "loading" });

    useEffect(() => {
        const controller = new AbortController();
        async function loadCard() {
            setResult({ state: "loading" });
            try {
                const response = await fetch("http://localhost:8080/api/customers/card", {
                    credentials: "include",
                    cache: "no-store",
                    signal: controller.signal,
                });
                if (response.status === 401 || response.status === 403) {
                    setResult({ state: "unauthenticated" });
                    return;
                }
                const data = await response.json().catch(() => null);
                if (controller.signal.aborted) return;
                if (response.status === 404) {
                    setResult({ state: "empty", message: data?.massage || "No debit card is available for your account." });
                    return;
                }
                if (!response.ok) {
                    setResult({ state: "error", message: "Unable to load your card. Please try again later." });
                    return;
                }
                if (!data || !/^[0-9]{16}$/.test(data.cardNumber) || !/^[0-9]{4}$/.test(data.lastFour) || !/^[0-9]{3}$/.test(data.cvc2)
                    || typeof data.cardHolderName !== "string" || data.type !== "DEBIT"
                    || !/^\d{4}-(0[1-9]|1[0-2])-\d{2}$/.test(data.expiryDate)) {
                    setResult({ state: "error", message: "The card response is incomplete. Please try again later." });
                    return;
                }
                setResult({ state: "ready", card: data });
            } catch {
                if (!controller.signal.aborted) {
                    setResult({ state: "error", message: "Unable to load your card. Check your connection and try again." });
                }
            }
        }
        loadCard();
        return () => controller.abort();
    }, [customer?.id]);

    return (
        <div className="my-card-page">
            <Header page="my-card" customer={customer} />
            <div className="my-card-body">
                <SideBar />
                <main className="my-card-main">
                    <div className="my-card-heading"><span className="my-card-eyebrow">YOUR EVERYDAY BANKING</span><h1>My Card</h1>
                    <p>Your debit card, all in one place.</p></div><section className="my-card-stage" aria-label="Your debit card">
                    {result.state === "loading" && <p role="status">Loading your card...</p>}
                    {result.state === "unauthenticated" && <p>Please <Link to="/login">log in</Link> to view your card.</p>}
                    {result.state === "empty" && <p role="status">{result.message}</p>}
                    {result.state === "error" && <p role="alert">{result.message}</p>}
                    {result.state === "ready" && (
                        <BankCard cardHolderName={result.card.cardHolderName}
                            cardNumber={result.card.cardNumber} lastFour={result.card.lastFour}
                            validDate={`${result.card.expiryDate.slice(5, 7)}/${result.card.expiryDate.slice(2, 4)}`}
                            cvc2={result.card.cvc2} type="Debit" />
                    )}
                    </section><p className="my-card-caption">Linked to your Main Account</p>
                </main>
            </div>
            <Footer />
        </div>
    );
}

export default MyCard;
