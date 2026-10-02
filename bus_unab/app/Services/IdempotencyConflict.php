<?php

namespace App\Services;

use RuntimeException;

class IdempotencyConflict extends RuntimeException {}
