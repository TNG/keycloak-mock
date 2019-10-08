import * as React from 'react';
import axios from 'axios';

const endpoint = 'http://localhost:8080';

export default class Hello extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      message: ""
    }
  }

  componentDidMount() {
    console.log('component did mount');
     axios.get(endpoint + '/hello').then(response => {
       console.log('response ' + response);
         this.setState({
           message: response.data
         })
       }
     ).catch(err => {
       this.setState({
         message: err.message
       })
     })
  }

  render() {
    return (
      <p> Server says: {this.state.message} ! </p>
    )
  }

}
