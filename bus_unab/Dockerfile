FROM php:8.3-fpm-alpine AS base

RUN apk add --no-cache \
        bash \
        curl \
        freetype-dev \
        libjpeg-turbo-dev \
        libpng-dev \
        libzip-dev \
        oniguruma-dev \
        icu-dev \
        linux-headers \
        $PHPIZE_DEPS \
    && docker-php-ext-configure gd --with-freetype --with-jpeg \
    && docker-php-ext-install -j$(nproc) \
        bcmath \
        exif \
        gd \
        intl \
        mbstring \
        opcache \
        pdo_mysql \
        pcntl \
        zip \
    && pecl install redis \
    && docker-php-ext-enable redis \
    && apk del $PHPIZE_DEPS \
    && rm -rf /tmp/pear /var/cache/apk/*

COPY --from=composer:2.8 /usr/bin/composer /usr/bin/composer

WORKDIR /var/www/html

# ── Composer deps ──────────────────────────────────────────────────────────────
FROM base AS deps

COPY composer.json composer.lock ./
RUN composer install \
    --no-dev \
    --no-interaction \
    --prefer-dist \
    --optimize-autoloader \
    --no-scripts

# ── Node / Vite build ──────────────────────────────────────────────────────────
FROM node:20-alpine AS node-build

WORKDIR /var/www/html
COPY package*.json ./
RUN npm ci --no-audit --no-fund
COPY resources/ resources/
COPY vite.config.js tailwind.config.js* postcss.config.js* ./
RUN npm run build

# ── Production image ───────────────────────────────────────────────────────────
FROM base AS production

COPY --from=deps /var/www/html/vendor ./vendor
COPY . .
COPY --from=node-build /var/www/html/public/build ./public/build

RUN mkdir -p \
        storage/framework/sessions \
        storage/framework/views \
        storage/framework/cache/data \
        storage/logs \
        bootstrap/cache \
        /var/log/php \
    && chown -R www-data:www-data /var/www/html /var/log/php \
    && chmod -R 775 storage bootstrap/cache

COPY docker/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

EXPOSE 9000

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
CMD ["php-fpm"]
