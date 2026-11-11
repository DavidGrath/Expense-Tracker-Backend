package com.davidgrath.expensetrackerbackend.service.exceptions

class CountLimitExceededException(val maxCount: Int): RuntimeException(), ServiceLayerException {
}