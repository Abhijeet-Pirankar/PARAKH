import React, { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, useLocation } from 'react-router-dom';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import Home from './pages/Home';
import CheckOffer from './pages/CheckOffer';
import RiskReport from './pages/RiskReport';

// Scroll to top on route change
function ScrollToTop() {
  const { pathname, hash } = useLocation();

  useEffect(() => {
    if (!hash) {
      window.scrollTo(0, 0);
    } else {
      const element = document.getElementById(hash.replace('#', ''));
      if (element) {
        element.scrollIntoView({ behavior: 'smooth' });
      }
    }
  }, [pathname, hash]);

  return null;
}

function App() {
  return (
    <Router>
      <ScrollToTop />
      <Navbar />
      <main className="main-content pt-16">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/check" element={<CheckOffer />} />
          <Route path="/report" element={<RiskReport />} />
          <Route path="/results" element={<RiskReport />} />
          <Route path="/investigation" element={<RiskReport />} />
        </Routes>
      </main>
      <Footer />
    </Router>
  );
}

export default App;
