# Domain Model

This document describes the current domain model of the digital banking system.
The model will evolve as new requirements are introduced.

## Overview

A customer represents a registered person in the bank.
Each customer has one or more bank accounts and one password credential.

An account belongs to one customer and has a status that indicates whether
the account is active, frozen, or closed.

## Diagram

![Domain Model](../images/domain-model.png)