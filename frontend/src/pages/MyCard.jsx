import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import Header from "../components/Header.jsx";
import Footer from "../components/Footer.jsx";
import SideBar from "../components/SideBar.jsx";
import BankCard from "../components/BankCard.jsx";
import "../style/MyCard.css";

function MyCard({ customer , logout}) {
    const [pinOpen, setPinOpen] = useState(false);
    const [pinPassword, setPinPassword] = useState('');
    const [revealedPin, setRevealedPin] = useState('');
    const [pinError, setPinError] = useState('');
    const [pinBusy, setPinBusy] = useState(false);
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
                setPinOpen(false); setPinPassword(''); setRevealedPin(''); setPinError('');
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

    useEffect(() => {
        if (!revealedPin) return;
        const timer = setTimeout(() => setRevealedPin(''), 30000);
        return () => clearTimeout(timer);
    }, [revealedPin]);

    async function revealPin(event) {
        event.preventDefault();
        if (pinBusy) return;
        setPinBusy(true); setPinError('');
        const password = pinPassword;
        setPinPassword(''); setRevealedPin('');
        try {
            const tokenResponse = await fetch('http://localhost:8080/api/customers/csrf', { credentials: 'include' });
            if (!tokenResponse.ok) throw new Error('Unable to verify your security session. Please reload.');
            const csrf = await tokenResponse.json();
            if (!csrf.token || !csrf.headerName) throw new Error('Unable to verify your security session. Please reload.');
            const response = await fetch('http://localhost:8080/api/customers/card/pin', {
                method: 'POST', credentials: 'include', cache: 'no-store',
                headers: { 'Content-Type': 'application/json', [csrf.headerName]: csrf.token },
                body: JSON.stringify({ password }),
            });
            const data = await response.json().catch(() => null);
            if (!response.ok) throw new Error(response.status === 401 ? 'Incorrect application password.'
                : response.status === 403 ? 'Please sign in again and retry.'
                    : response.status < 500 && typeof data?.massage === 'string' ? data.massage : 'Unable to show your PIN.');
            if (!/^[0-9]{4}$/.test(data?.pin)) throw new Error('Unable to read your PIN.');
            setRevealedPin(data.pin); setPinOpen(false);
        } catch (failure) {
            setPinError(failure instanceof TypeError ? 'Unable to connect. Please try again.' : failure.message);
        } finally { setPinBusy(false); }
    }

    return (
        <div className="my-card-page">
            <Header logout={logout} page="my-card" customer={customer} />
            <div className="my-card-body">
                <SideBar logout={logout} />
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
                    {result.state === "ready" && <section className="my-card-pin" aria-label="Card PIN">
                        {revealedPin ? <>
                            <p>Your card PIN</p><output className="my-card-pin-value">{revealedPin}</output>
                            <p>Hides automatically after 30 seconds.</p>
                            <button type="button" onClick={() => setRevealedPin('')}>Hide PIN</button>
                        </> : pinOpen ? <form onSubmit={revealPin}>
                            <label htmlFor="pin-password">Enter your URBank application password to show your card PIN</label>
                            <input id="pin-password" type="password" autoComplete="current-password" required value={pinPassword}
                                disabled={pinBusy} onChange={event => setPinPassword(event.target.value)} />
                            <button type="submit" disabled={pinBusy}>{pinBusy ? 'Verifying…' : 'Show PIN'}</button>
                            <button type="button" disabled={pinBusy} onClick={() => { setPinOpen(false); setPinPassword(''); setPinError(''); }}>Cancel</button>
                        </form> : <button type="button" onClick={() => { setPinOpen(true); setPinError(''); }}>Show PIN</button>}
                        {pinError && <p role="alert">{pinError}</p>}
                    </section>}
                </main>
            </div>
            <Footer />
        </div>
    );
}

export default MyCard;
