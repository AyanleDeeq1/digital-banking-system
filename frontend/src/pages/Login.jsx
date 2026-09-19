function Login() {
    return (
        <div>
            <form action="">
                <label htmlFor="email">Enter your email</label>
                <input type="email" name="email" id="email" placeholder="janedoe@email.com"/> <br />
                <label htmlFor="password">Enter passowrd</label>
                <input type="password" name="password" id="password" />
            </form>
        </div>
    )
}

export default Login