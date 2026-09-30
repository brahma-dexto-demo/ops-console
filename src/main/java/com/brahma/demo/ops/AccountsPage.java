package com.brahma.demo.ops;

import java.util.List;

public record AccountsPage(List<Account> accounts, int total, int limit, int offset) {
}
