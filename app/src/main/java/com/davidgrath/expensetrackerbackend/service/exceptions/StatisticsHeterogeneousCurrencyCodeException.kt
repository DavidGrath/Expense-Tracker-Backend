package com.davidgrath.expensetrackerbackend.service.exceptions

/**
 * Statistics filters require exactly 1 currency code across all the accounts provided
 */
class StatisticsHeterogeneousCurrencyCodeException(): RuntimeException(), ServiceLayerException