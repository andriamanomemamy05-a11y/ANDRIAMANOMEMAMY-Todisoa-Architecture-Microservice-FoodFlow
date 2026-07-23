import { Module } from '@nestjs/common';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { PaymentsModule } from './payments/payments.module';
import { KafkaModule } from './kafka/kafka.module';

@Module({
  imports: [PaymentsModule, KafkaModule],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}
