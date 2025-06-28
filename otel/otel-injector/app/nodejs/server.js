const express = require('express');
const app = express();
const port = process.env.PORT || 3000;

// Middleware for JSON parsing
app.use(express.json());

// Simulate some processing delay
const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms));

// Root endpoint
app.get('/', async (req, res) => {
  console.log('GET / - Home endpoint hit');
  await delay(Math.random() * 100 + 50); // 50-150ms delay
  res.json({ 
    message: 'Hello from OpenTelemetry Injector Demo!',
    timestamp: new Date().toISOString(),
    service: 'nodejs-server'
  });
});

// API endpoint with some business logic
app.get('/api/users', async (req, res) => {
  console.log('GET /api/users - Users endpoint hit');
  await delay(Math.random() * 200 + 100); // 100-300ms delay
  
  const users = [
    { id: 1, name: 'Alice', email: 'alice@example.com' },
    { id: 2, name: 'Bob', email: 'bob@example.com' },
    { id: 3, name: 'Charlie', email: 'charlie@example.com' }
  ];
  
  res.json({ 
    users, 
    timestamp: new Date().toISOString(),
    service: 'nodejs-server'
  });
});

// API endpoint with potential error
app.get('/api/orders', async (req, res) => {
  console.log('GET /api/orders - Orders endpoint hit');
  await delay(Math.random() * 150 + 75); // 75-225ms delay
  
  // Simulate random errors
  if (Math.random() < 0.1) { // 10% error rate
    console.error('Simulated error in /api/orders');
    return res.status(500).json({ 
      error: 'Internal server error',
      timestamp: new Date().toISOString(),
      service: 'nodejs-server'
    });
  }
  
  const orders = [
    { id: 101, userId: 1, product: 'Laptop', amount: 999.99 },
    { id: 102, userId: 2, product: 'Mouse', amount: 29.99 },
    { id: 103, userId: 3, product: 'Keyboard', amount: 79.99 }
  ];
  
  res.json({ 
    orders, 
    timestamp: new Date().toISOString(),
    service: 'nodejs-server'
  });
});

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ 
    status: 'healthy',
    timestamp: new Date().toISOString(),
    uptime: process.uptime(),
    service: 'nodejs-server'
  });
});

app.listen(port, '0.0.0.0', () => {
  console.log(`Server running on http://0.0.0.0:${port}`);
  console.log('Available endpoints:');
  console.log('  GET /');
  console.log('  GET /api/users');
  console.log('  GET /api/orders');
  console.log('  GET /health');
}); 