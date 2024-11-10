# S-Gate: QR-Based Login System
## Overview
### S-Gate is a secure and convenient login solution that uses QR codes for quick and temporary access to websites.
### Instead of entering a username and password, users simply scan a QR code to authenticate their session.
### This project is designed to offer seamless, cross-platform login capabilities that enhance both security and user experience.

# Why S-Gate?     
### Traditional login systems often require users to remember multiple passwords, 
### which can be inconvenient and sometimes insecure. S-Gate addresses this challenge by enabling secure, 
### token-based authentication without needing to store account details on each device.
### The system also leverages Firebase Realtime Database and JWT (JSON Web Tokens) for real-time communication and secure token verification, ensuring user sessions are protected.

# Features
## List key features, such as:

* ### QR code scanning for login
* ### Firebase Realtime Database integration
* ### Secure token verification with JWT (JSON Web Token)

# How It Works
* ### The website generates a unique QR code containing a login token.
* ### The user scans the QR code using the S-Gate mobile app.
* ### The mobile app sends the scanned token to the backend server.
* ### The server verifies the token with Firebase Realtime Database.
* ### Upon successful verification, the website grants the user access and displays a login success message.

![ezgif-6-e3a10944ab](https://github.com/user-attachments/assets/ccbe23ac-aa01-4da2-b485-588326cfcf1d)
