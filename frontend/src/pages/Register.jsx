import { useState } from "react"
import { useNavigate } from "react-router-dom";
import Header from '../components/Header.jsx';
import Footer from '../components/Footer.jsx'
import "../style/Register.css";
import Button from "../components/Button.jsx";

function Register({ csrfToken, customer, logout}) {
    const [firstName, setFirstName] = useState('')
    const [lastName, setLastName] = useState('')
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [confirmPassword, setConfirmPassword] = useState('');
    const [touched, setTouched] = useState({});
    const [fieldErrors, setFieldErrors] = useState({});
    const [error, setError] = useState('');
    const [submitting, setSubmitting] = useState(false);
    const navigate = useNavigate();
    const requirements = [
        { label: 'At least 8 characters', valid: password.length >= 8 },
        { label: 'One uppercase letter', valid: /[A-Z]/.test(password) },
        { label: 'One lowercase letter', valid: /[a-z]/.test(password) },
        { label: 'One number', valid: /[0-9]/.test(password) },
        { label: 'One special character', valid: /[\p{P}\p{S}]/u.test(password) },
    ];
    const passwordValid = requirements.every(requirement => requirement.valid);
    const emailValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
    const passwordsMatch = confirmPassword.length > 0 && confirmPassword === password;
    const formValid = firstName.trim() && lastName.trim() && emailValid && passwordValid && passwordsMatch;

    function changeField(name, setter, value) {
        setter(value);
        setTouched(previous => ({ ...previous, [name]: true }));
        setFieldErrors(previous => ({ ...previous, [name]: undefined }));
        setError('');
    }

    function fieldFeedback(name) {
        return fieldErrors[name]?.length > 0 && (
            <p id={`${name}-server-error`} className="validation-feedback invalid" role="alert">
                ✕ {fieldErrors[name].join('. ')}
            </p>
        );
    }

    async function handleSubmit(event) {
        event.preventDefault()
        setTouched({ firstName: true, lastName: true, email: true, password: true, confirmPassword: true });
        if (!formValid || submitting) return;
        if (!csrfToken) {
            setError('Unable to start registration. Please refresh the page and try again.');
            return;
        }
        setSubmitting(true);
        setError('');
        setFieldErrors({});

        const customer = {
            firstName,
            lastName,
            email,
            password
        }

        try {
        const request = await fetch('http://localhost:8080/api/customers', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': csrfToken
            },
            credentials: "include",
            body: JSON.stringify(customer)
        })
        if (request.ok) {
            navigate('/login');
            return;
        }
        const response = await request.json().catch(() => null);
        if (request.status === 409) {
            setFieldErrors({ email: ['An account with this email already exists.'] });
        } else if (request.status === 400) {
            const errors = {};
            for (const name of ['firstName', 'lastName', 'email', 'password']) {
                const messages = response?.fieldErrors?.[name];
                if (Array.isArray(messages)) errors[name] = messages.filter(message => typeof message === 'string');
            }
            setFieldErrors(errors);
            setError(response?.massage === 'Please check the highlighted fields.'
                ? response.massage : 'Please check your registration details and try again.');
        } else if (request.status === 401 || request.status === 403) {
            setError('Your security session could not be verified. Please refresh the page and try again.');
        } else {
            setError('Registration could not be completed. Please try again later.');
        }
        } catch {
            setError('Unable to reach the server. Check your connection and try again.');
        } finally {
            setSubmitting(false);
        }
    }
    return (
        <div className="register-page">

            <Header logout={logout} page="register" customer={customer} />

            <main className="register-main">

                <section className="register-card">

                    <div className="register-heading">
                        <p className="register-eyebrow">GET STARTED</p>

                        <h1>Create your URBank account</h1>

                        <p className="register-subtitle">
                            Simple, secure banking starts here.
                        </p>
                    </div>

                    <form onSubmit={handleSubmit} noValidate aria-busy={submitting}>

                        <div className="register-name-row">

                            <div className="form-group">
                                <label htmlFor="firstName">First name</label>
                                <input
                                    type="text"
                                    name="firstName"
                                    id="firstName"
                                    placeholder="Jane"
                                    value={firstName} required disabled={submitting} autoComplete="given-name"
                                    aria-invalid={Boolean((touched.firstName && !firstName.trim()) || fieldErrors.firstName?.length)}
                                    aria-describedby="firstName-feedback firstName-server-error"
                                    onBlur={() => setTouched(previous => ({ ...previous, firstName: true }))}
                                    onChange={(event) => changeField('firstName', setFirstName, event.target.value)}
                                />
                                {touched.firstName && !firstName.trim() && <p id="firstName-feedback" className="validation-feedback invalid">✕ First name is required</p>}
                                {fieldFeedback('firstName')}
                            </div>

                            <div className="form-group">
                                <label htmlFor="lastName">Last name</label>
                                <input
                                    type="text"
                                    name="lastName"
                                    id="lastName"
                                    placeholder="Doe"
                                    value={lastName} required disabled={submitting} autoComplete="family-name"
                                    aria-invalid={Boolean((touched.lastName && !lastName.trim()) || fieldErrors.lastName?.length)}
                                    aria-describedby="lastName-feedback lastName-server-error"
                                    onBlur={() => setTouched(previous => ({ ...previous, lastName: true }))}
                                    onChange={(event) => changeField('lastName', setLastName, event.target.value)}
                                />
                                {touched.lastName && !lastName.trim() && <p id="lastName-feedback" className="validation-feedback invalid">✕ Last name is required</p>}
                                {fieldFeedback('lastName')}
                            </div>

                        </div>

                        <div className="form-group">
                            <label htmlFor="email">Email address</label>
                            <input
                                type="email"
                                name="email"
                                id="email"
                                placeholder="janedoe@email.com"
                                value={email} required disabled={submitting} autoComplete="email"
                                aria-invalid={Boolean((touched.email && !emailValid) || fieldErrors.email?.length)}
                                aria-describedby="email-feedback email-server-error"
                                onBlur={() => setTouched(previous => ({ ...previous, email: true }))}
                                onChange={(event) => changeField('email', setEmail, event.target.value)}
                            />
                            {touched.email && !fieldErrors.email?.length && <p id="email-feedback" className={`validation-feedback ${emailValid ? 'valid' : 'invalid'}`} aria-live="polite">
                                {emailValid ? '✓ Valid email address' : '✕ Enter a valid email address'}
                            </p>}
                            {fieldFeedback('email')}
                        </div>

                        <div className="form-group">
                            <label htmlFor="password">Password</label>
                            <input
                                type="password"
                                name="password"
                                id="password"
                                placeholder="Create a password"
                                value={password} required disabled={submitting} autoComplete="new-password"
                                aria-invalid={Boolean((touched.password && !passwordValid) || fieldErrors.password?.length)}
                                aria-describedby="password-requirements password-server-error"
                                onChange={(event) => changeField('password', setPassword, event.target.value)}
                            />
                            <ul id="password-requirements" className="password-requirements" aria-live="polite">
                                {requirements.map(requirement => (
                                    <li key={requirement.label} className={!touched.password ? 'neutral' : requirement.valid ? 'valid' : 'invalid'}>
                                        <span aria-hidden="true">{!touched.password ? '•' : requirement.valid ? '✓' : '✕'}</span>{' '}
                                        <span className="validation-sr-only">{!touched.password ? 'Required: ' : requirement.valid ? 'Satisfied: ' : 'Not satisfied: '}</span>
                                        {requirement.label}
                                    </li>
                                ))}
                            </ul>
                            {fieldFeedback('password')}
                        </div>

                        <div className="form-group">
                            <label htmlFor="confirmPassword">Confirm password</label>
                            <input id="confirmPassword" name="confirmPassword" type="password" autoComplete="new-password"
                                value={confirmPassword} required disabled={submitting} placeholder="Re-enter your password"
                                aria-invalid={Boolean(touched.confirmPassword && !passwordsMatch)} aria-describedby="confirmation-feedback"
                                onChange={event => changeField('confirmPassword', setConfirmPassword, event.target.value)} />
                            {touched.confirmPassword && <p id="confirmation-feedback" className={`validation-feedback ${passwordsMatch ? 'valid' : 'invalid'}`} aria-live="polite">
                                {passwordsMatch ? '✓ Passwords match' : '✕ Passwords do not match'}
                            </p>}
                        </div>
                        {error && <p className="validation-feedback invalid" role="alert">{error}</p>}
                        <Button type="submit" disabled={!formValid || submitting}>
                                {submitting ? 'Creating account...' : 'Create account'}
                        </Button>

                    </form>

                </section>

            </main>

            <Footer />

        </div>
    );
}
export default Register
