// A local preview must never expose the repository (including ignored credentials) as a static root.
if (config.devServer) {
    config.devServer.host = '127.0.0.1';
    config.devServer.allowedHosts = ['localhost', '127.0.0.1'];
    config.devServer.historyApiFallback = true;
    config.devServer.static = (config.devServer.static || []).filter(entry => {
        const directory = (typeof entry === 'string' ? entry : entry.directory).replace(/\\/g, '/');
        return directory.endsWith('/StreamCoreTV-webApp/kotlin') ||
            directory.endsWith('/webApp/build/processedResources/wasmJs/main') ||
            directory.endsWith('/webApp/build/generated/webDevelopmentConfig');
    });
}
