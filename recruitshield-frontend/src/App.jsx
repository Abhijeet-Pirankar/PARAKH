import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import Home from './pages/Home';
import CheckOffer from './pages/CheckOffer';

function App() {
  return (
    <Router>
      <Navbar />
      <main className="main-content">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/check" element={<CheckOffer />} />
        </Routes>
      </main>
      <Footer />
    </Router>
  );
}

export default App;
