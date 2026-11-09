package com.davidgrath.expensetrackerbackend.service.exceptions

class CountLimitExceeded(val maxCount: Int): RuntimeException(), ServiceLayerException {
}